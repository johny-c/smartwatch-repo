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

/* The Bluetooth includes */
#include <sys/socket.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/l2cap.h>

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

static int pixel = -1; /* pointsize in pixel */
static int pgap = 0; /* gap between points */
static int rgap = 0; /* row gap between lines */
static int cgap = 0; /* column gap between characters */
static int border = 0; /* window border */

static int dimx, dimy; /* total window dimension in pixel */

static RGBA BC;
static RGBA *drv_BT_FB = NULL;

static int dirty = 1;

static int btSocket = 0;
static int connected = 0;

/*******************************/
/***  bluetooth functions    ***/
/*******************************/
static int drv_BT_socket_init(void) {
	struct sockaddr_l2 addr = { 0 };
	char dest[18] = "....";

	/* allocate socket */
	btSocket = socket(AF_BLUETOOTH, SOCK_SEQPACKET, BTPROTO_L2CAP);

	/* bind socket to port 0x1001 of the first available */
	/* bluetooth adapter */
	addr.l2_family = AF_BLUETOOTH;
	addr.l2_bdaddr = *BDADDR_ANY;
	addr.l2_psm = htobs(0x1001);
	str2ba( dest, &addr.l2_bdaddr );

	// connect to server
	if (connect(btSocket, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
		error("Error while connecting to smartphone");
		return -1;
	}

	connected = 1;

	return 0;
}

static int drv_BT_socket_push(void* data, int len) {
	if (connected == 0) {
		return 0;
	}

	if (len == 0) {
		return 0;
	}

	int res;
	res = write(btSocket, data, len);

	if (res < 0) {
		error("Unable to write all data to the bluetooth stream");
		return -1;
	}

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

static void drv_BT_flush(void) {
	static RGBA *bitbuf = NULL;
	int xsize, ysize, row, col, i;

	xsize = 2 * border + (DCOLS / XRES - 1) * cgap + DCOLS * pixel
			+ (DCOLS - 1) * pgap;
	ysize = 2 * border + (DROWS / YRES - 1) * rgap + DROWS * pixel
			+ (DROWS - 1) * pgap;

	if (bitbuf == NULL) {
		if ((bitbuf = malloc(xsize * ysize * sizeof(RGBA))) == NULL) {
			error("%s: malloc() failed: %s", Name, strerror(errno));
			return;
		}
	}

	for (i = 0; i < xsize * ysize; i++) {
		bitbuf[i] = BC;
	}

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
		if (strlen(output) != 18) { /* Probably only 17 when \0 don't count */
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

	/* Initialize the bluetooth device */
	drv_BT_socket_init();

	/* initially flush the image to a file */
	drv_BT_flush();

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
