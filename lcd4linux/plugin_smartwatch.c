/*
 * Plugin Smartwatch
 */

#include "config.h"

/* these should always be included */
#include "debug.h"
#include "plugin.h"
#include "cfg.h"

#include <time.h>
#include <stdlib.h>


static char Section[] = "Plugin:smartwatch";
static int plugin_enabled;


/* Note: all local functions should be declared 'static' */

/* Get Time and Date */
static void my_smartwatch(RESULT * result)
{
	time_t value;

	value = time(NULL);

	SetResult(&result, R_NUMBER, ctime(&value));
}

/* plugin initialization */
/* MUST NOT be declared 'static'! */
int plugin_init_smartwatch(void)
{
    /* Check if smartwatch plugin section exists in config file */
    if (cfg_number(Section, "enabled", 0, 0, 1, &plugin_enabled) < 1)
    {
    	plugin_enabled = 0;
    }

    /* Disable plugin unless it is explicitly enabled */
    if (plugin_enabled != 1)
    {
    	info("[smartwatch] WARNING: Plugin is not enabled! (set 'enabled 1' to enable this plugin)");
    	return 0;
    }

    AddFunction("Time::", 0, my_smartwatch);

    return 0;
}

void plugin_exit_smartwatch(void)
{
    /* free any allocated memory */
    /* close filedescriptors */
}
