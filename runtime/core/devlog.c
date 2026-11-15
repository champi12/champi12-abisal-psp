/* devlog: boot and runtime diary written to devlog.txt next to the game. */
#include <stdarg.h>
#include <stdio.h>
#include "plat.h"

static int64_t t0;

void devlog(const char *fmt, ...) {
    if (!t0) t0 = plat_ticks_us();
    char line[512];
    int64_t us = plat_ticks_us() - t0;
    int n = snprintf(line, sizeof line, "[%4d.%03d] ", (int)(us / 1000000), (int)(us / 1000 % 1000));
    va_list ap;
    va_start(ap, fmt);
    n += vsnprintf(line + n, sizeof line - (size_t)n - 2, fmt, ap);
    va_end(ap);
    if (n > (int)sizeof line - 2) n = (int)sizeof line - 2;
    line[n++] = '\n';
    plat_log(line, n);
}
