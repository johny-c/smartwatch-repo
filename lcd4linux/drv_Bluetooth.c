/* $Id: drv_Image.c 840 2007-09-09 12:17:42Z michael $
 * $URL: https://ssl.bulix.org/svn/lcd4linux/trunk/drv_Image.c $
 *
 * new style Image (PPM/PNG) Driver for LCD4Linux 
 *
 * Copyright (C) 2003 Michael Reinelt <michael@reinelt.co.at>
 * Copyright (C) 2004 The LCD4Linux Team <lcd4linux-devel@users.sourceforge.net>
 *
 * This file is part of LCD4Linux.
 *
 * LCD4Linux is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * LCD4Linux is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

/* 
 *
 * exported fuctions:
 *
 * struct DRIVER drv_Image
 *
 */

#include "config.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <sys/time.h>
//#include <time.h>

/* The Bluetooth includes */
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/l2cap.h>
#include <bluetooth/rfcomm.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>

#include "debug.h"
#include "cfg.h"
#include "timer.h"
#include "qprintf.h"
#include "plugin.h"
#include "drv.h"
#include "drv_generic_graphic.h"

#ifdef WITH_DMALLOC
#include <dmalloc.h>
#endif

static char Name[] = "Bluetooth";
const static char SMARTPHONE_BT_MAC[] = "D0:C1:B1:1D:63:0F";

static int pixel = -1; /* pointsize in pixel */
static int pgap = 0; /* gap between points */
static int rgap = 0; /* row gap between lines */
static int cgap = 0; /* column gap between characters */
static int border = 0; /* window border */

static int dimx, dimy; /* total window dimension in pixel */

static RGBA BC;
static RGBA RANDOM_COLOR;
static RGBA *drv_BT_FB = NULL;

static int dirty = 1;

static int btSocket = 0;
static int connected = 0;


/*******************************/
/***  bluetooth functions    ***/
/*******************************/

// Find Service Discovery Protocol Server
static uint8_t drv_BT_find_SDP(void) {

	//char SECURE_UUID[] = "d2e19fab-f367-43d9-9cc2-b6837e7eb915";
	//uint32_t svc_uuid_int[] = { 0 , 0 , 0 , 0xABCD } ;
    uint8_t svc_uuid_int[] = { 0xd2, 0xe1, 0x9f, 0xab,
    						   0xf3, 0x67, 0x43, 0xd9,
    						   0x9c, 0xc2, 0xb6, 0x83,
    						   0x7e, 0x7e, 0xb9, 0x15 };
    uuid_t svc_uuid;
    int err;
    bdaddr_t target;
    sdp_list_t *response_list = NULL, *search_list, *attrid_list;
    sdp_session_t *session = 0;
    uint8_t port = 0;

    str2ba( SMARTPHONE_BT_MAC, &target );

    // connect to the SDP server running on the remote machine
    session = sdp_connect( BDADDR_ANY, &target, SDP_RETRY_IF_BUSY );

    // specify the UUID of the application we're searching for
    sdp_uuid128_create( &svc_uuid, &svc_uuid_int );
    search_list = sdp_list_append( NULL, &svc_uuid );

    // specify that we want a list of all the matching applications' attributes
    uint32_t range = 0x0000ffff;
    attrid_list = sdp_list_append( NULL, &range );

    // get a list of service records that have our UUID
    err = sdp_service_search_attr_req( session, search_list,
    		SDP_ATTR_REQ_RANGE, attrid_list, &response_list);

    sdp_list_t *r = response_list;

    // go through each of the service records
    for (; r; r = r->next ) {
        sdp_record_t *rec = (sdp_record_t*) r->data;
        sdp_list_t *proto_list;

        // get a list of the protocol sequences
        if( sdp_get_access_protos( rec, &proto_list ) == 0 ) {

			// get the RFCOMM port number
			port = sdp_get_proto_port( proto_list , RFCOMM_UUID ) ;

			sdp_list_t *p = proto_list;

			// go through each protocol sequence
			for( ; p ; p = p->next ) {
				sdp_list_t *pds = (sdp_list_t*)p->data;

				// go through each protocol list of the protocol sequence
				for( ; pds ; pds = pds->next ) {

					// check the protocol attributes
					sdp_data_t *d = (sdp_data_t*)pds->data;
					int proto = 0;
					for( ; d; d = d->next ) {
						switch( d->dtd ) {
							case SDP_UUID16:
							case SDP_UUID32:
							case SDP_UUID128:
								proto = sdp_uuid_to_proto( &d->val.uuid );
								break;
							case SDP_UINT8:
								if( proto == RFCOMM_UUID ) {
									printf("Found SDP on rfcomm channel: %d\n",d->val.int8);
								}
								break;
						}
					}
				}
				sdp_list_free( (sdp_list_t*)p->data, 0 );
			}
			sdp_list_free( proto_list, 0 );

        }

        printf("found service record 0x%x\n", rec->handle);
        sdp_record_free( rec );
    }

	sdp_list_free( response_list, 0 );
	sdp_list_free( search_list, 0 );
	sdp_list_free( attrid_list, 0 );
    sdp_close(session);

	if( port != 0 ){
		printf( "Found service running on RFCOMM port %d\n\n" , port ) ;
	}
    return port;
}



// Initialize Socket Connection
static int drv_BT_socket_init(uint8_t port) {
	//struct sockaddr_l2 addr = { 0 };
	struct sockaddr_rc addr = { 0 };
	//char dest[18] = "....";

	/* allocate socket */
	//btSocket = socket(AF_BLUETOOTH, SOCK_SEQPACKET, BTPROTO_L2CAP);
	btSocket = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);

	/* bind socket to port 0x1001 of the first available */
	/* bluetooth adapter */
	//addr.l2_family = AF_BLUETOOTH;
	//addr.l2_bdaddr = *BDADDR_ANY;
	//addr.l2_psm = htobs(0x1001);
	//str2ba( dest, &addr.l2_bdaddr );
	addr.rc_family = AF_BLUETOOTH;
    addr.rc_channel = (uint8_t) port; // was 1, 0 is supposed to find the first available port
    str2ba( SMARTPHONE_BT_MAC, &addr.rc_bdaddr );
    //str2ba( output, &addr.rc_bdaddr );

	// connect to server
	if (connect(btSocket, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
		error("Error while connecting to smartphone");
		return -1;
	}
	else {
		printf("Successfully connected to smartphone!\n");
		connected = 1;
	}
	return 0;
}

static int drv_BT_socket_push(void* data, int len) {
	if (connected == 0) {
		error("Not connected!");
		return 0;
	}

	if (len == 0) {
		error("Length of data to push = 0!");
		return 0;
	}

	int res;
	res = write(btSocket, data, len);

	if (res < 0) {
		error("Unable to write all data to the bluetooth stream");
		return -1;
	}


	time_t rawtime;
	struct tm * timeinfo;

	time ( &rawtime );
	timeinfo = localtime ( &rawtime );

	printf("Pushed a frame of len= %d , at %s", len, asctime(timeinfo));
	return 0;
}

static int drv_BT_socket_quit(void) {
	connected = 0;
	close(btSocket);

	return 0;
}

/****************************************/
/***  hardware dependant functions    ***/
/****************************************/


int frameCounter = 0;

static void drv_BT_flush(void) {
	static RGBA *bitbuf = NULL;
	int xsize, ysize, row, col, i;

	//border = 20;
	//DCOLS = DROWS = 128;
	//XRES = 6;
	//YRES = 8;
	//cgap = 5;
	//rgap = 5;
	//pixel = 4;
	//pgap = 1;
	//xsize = 2 * 20 + (128/6 - 1)*5 + 128*4 + 127*1;
	//ysize = 2 * 20 + (128/8 - 1)*5 + 128*4 + 127*1;
	/*
	xsize = 2 * border + (DCOLS / XRES - 1) * cgap + DCOLS * pixel
			+ (DCOLS - 1) * pgap;
	ysize = 2 * border + (DROWS / YRES - 1) * rgap + DROWS * pixel
			+ (DROWS - 1) * pgap;


	printf("Border = %d\n", border);
	printf("DCOLS = %d, DROWS = %d\n", DCOLS, DROWS);
	printf("XRES = %d, YRES = %d\n", XRES, YRES);
	printf("cgap = %d, rgap = %d\n", cgap, rgap);
	printf("pixel = %d, pgap = %d\n", pixel, pgap);
	printf("xsize = %d,  ysize = %d\n\n", xsize, ysize);
	*/

	xsize = 128;
	ysize = 128;
	//printf("xsize = %d,  ysize = %d\n\n", xsize, ysize);

	if (bitbuf == NULL) {
		if ((bitbuf = malloc(xsize * ysize * sizeof(RGBA))) == NULL) {
			error("%s: malloc() failed: %s", Name, strerror(errno));
			return;
		}
	}


	RANDOM_COLOR.R = rand() % 256;
	RANDOM_COLOR.G = rand() % 256;
	RANDOM_COLOR.B = rand() % 256;
	RANDOM_COLOR.A = 255;
	printf("RANDOM_COLOR = (%d %d %d) , A = %d", RANDOM_COLOR.R, RANDOM_COLOR.G, RANDOM_COLOR.B, RANDOM_COLOR.A);
	for (i = 0; i < xsize * ysize; i++) {
		bitbuf[i] = RANDOM_COLOR; //BC;
	}

	/*
	for (row = 0; row < DROWS; row++) {
		int y = border + (row / YRES) * rgap + row * (pixel + pgap);
		for (col = 0; col < DCOLS; col++) {
			int x = border + (col / XRES) * cgap + col * (pixel + pgap);
			int a, b;
			for (a = 0; a < pixel; a++)
				for (b = 0; b < pixel; b++)
					bitbuf[y * xsize + x + a * xsize + b] = drv_BT_FB[row
							* DCOLS + col];
		}
	}
	*/

	/*
	for (row = 0; row < DROWS; row++) {
		for (col = 0; col < DCOLS; col++) {
			bitbuf[row][col] = drv_BT_FB[row][col];
		}
	}
	*/

	//printf("Len of buffer: %d\n", xsize * ysize * sizeof(RGBA)); // = 779 * 754 * 4
	//printf("Size of buffer: %d\n", sizeof(bitbuf)); // = 8
	if (drv_BT_socket_push(bitbuf, xsize * ysize * sizeof(RGBA)) < 0) {
		error("Incomplete bluetooth push");
		return;
	}

	return;
}

static void drv_BT_timer(__attribute__ ((unused))
void *notused) {
	if (dirty) {
		drv_BT_flush();
		dirty = 0;
	}
}

static void drv_BT_blit(const int row, const int col, const int height,
		const int width) {
	int r, c;

	for (r = row; r < row + height; r++) {
		for (c = col; c < col + width; c++) {
			RGBA p1 = drv_BT_FB[r * DCOLS + c];
			RGBA p2 = drv_generic_graphic_rgb(r, c);
			if (p1.R != p2.R || p1.G != p2.G || p1.B != p2.B) {
				drv_BT_FB[r * DCOLS + c] = p2;
				dirty = 1;
			}
		}
	}
}

static int drv_BT_start(const char *section) {
	int i, timeout;
	char *s;

	if (output == NULL || *output == '\0') {
		/* read bluetooth device from config */
		output = cfg_get(section, "Device", "00:00:00:00:00:00");
		printf("Bluetooth address: %s , length: %d.\n", output, strlen(output));
		if (strlen(output) != 17) { /* Probably only 17 when \0 don't count */
			error("%s: bad %s.Device '%s' from '%s'", Name, section, s, cfg_source());
		}
	}

	if (output == NULL || *output == '\0') {
		error("%s: no bluetooth device specified (use -o switch)", Name);
		return -1;
	}

	/* read display size from config */
	if (sscanf(s = cfg_get(section, "Size", "128x128"), "%dx%d", &DCOLS, &DROWS)
			!= 2 || DCOLS < 1 || DROWS < 1) {
		error("%s: bad %s.Size '%s' from %s", Name, section, s, cfg_source());
		free(s);
		return -1;
	}
	free(s);

	if (sscanf(s = cfg_get(section, "Font", "5x8"), "%dx%d", &XRES, &YRES) != 2
			|| XRES < 1 || YRES < 1) {
		error("%s: bad %s.Font '%s' from %s", Name, section, s, cfg_source());
		free(s);
		return -1;
	}
	free(s);

	if (sscanf(s = cfg_get(section, "Pixel", "4+1"), "%d+%d", &pixel, &pgap)
			!= 2 || pixel < 1 || pgap < 0) {
		error("%s: bad %s.Pixel '%s' from %s", Name, section, s, cfg_source());
		free(s);
		return -1;
	}
	free(s);

	if (sscanf(s = cfg_get(section, "Gap", "-1x-1"), "%dx%d", &cgap, &rgap) != 2
			|| cgap < -1 || rgap < -1) {
		error("%s: bad %s.Gap '%s' from %s", Name, section, s, cfg_source());
		free(s);
		return -1;
	}
	free(s);

	if (rgap < 0)
		rgap = pixel + pgap;
	if (cgap < 0)
		cgap = pixel + pgap;

	if (cfg_number(section, "Border", 0, 0, -1, &border) < 0)
		return -1;

	s = cfg_get(section, "Basecolor", "000000ff");
	if (color2RGBA(s, &BC) < 0) {
		error("%s: ignoring illegal color '%s'", Name, s);
	}
	free(s);

	if (cfg_number(section, "Time", 100, 1, 10000, &timeout)<0){
		error("%s: ignore illegal timeout", Name);
	}

	drv_BT_FB = malloc(DCOLS * DROWS * sizeof(*drv_BT_FB));
	if (drv_BT_FB == NULL) {
		error("%s: framebuffer could not be allocated: malloc() failed", Name);
		return -1;
	}

	for (i = 0; i < DCOLS * DROWS; i++) {
		drv_BT_FB[i] = BC;
	}

	dimx = DCOLS * pixel + (DCOLS - 1) * pgap + (DCOLS / XRES - 1) * cgap;
	dimy = DROWS * pixel + (DROWS - 1) * pgap + (DROWS / YRES - 1) * rgap;


	/* Find smartphone and the port that is open */
	uint8_t port = drv_BT_find_SDP();

	/* Initialize the bluetooth device */
	drv_BT_socket_init(port);

	/* initially flush the image to a file */
	drv_BT_flush();

	printf("Calling drv_BT_flush every %d milli seconds", timeout);
	/* regularly flush the image to a file */
	timer_add(drv_BT_timer, NULL, timeout, 0);

	return 0;
}

/****************************************/
/***            plugins               ***/
/****************************************/

/* none at the moment... */

/****************************************/
/***        exported functions        ***/
/****************************************/

/* list models */
int drv_BT_list(void) {
	printf("bluetooth ppm");
	return 0;
}

/* initialize driver & display */
int drv_BT_init(const char *section, const __attribute__ ((unused))
int quiet) {
	int ret;

	info("%s: %s", Name, "$Rev: 840 $");

	/* real worker functions */
	drv_generic_graphic_real_blit = drv_BT_blit;

	/* start display */
	if ((ret = drv_BT_start(section)) != 0)
		return ret;

	/* initialize generic graphic driver */
	if ((ret = drv_generic_graphic_init(section, Name)) != 0)
		return ret;

	/* register plugins */
	/* none at the moment... */

	return 0;
}

/* close driver & display */
int drv_BT_quit(const __attribute__ ((unused)) int quiet) {

	info("%s: shutting down.", Name);
	drv_generic_graphic_quit();

	drv_BT_socket_quit();

	if (drv_BT_FB) {
		free(drv_BT_FB);
		drv_BT_FB = NULL;
	}

	return (0);
}

DRIVER drv_Bluetooth = { .name = Name, .list = drv_BT_list, .init = drv_BT_init,
		.quit = drv_BT_quit, };
