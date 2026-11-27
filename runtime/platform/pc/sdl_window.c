/* Optional SDL window for the PC test build (--window): shows the software
 * framebuffer, maps the keyboard AND a gamepad to the PSP controls, and plays
 * the mixer's output. */
#include <SDL.h>
#include "plat.h"

/* defined in plat_pc.c */
void plat_push_event(int ev);
void plat_set_analog(int a);

static SDL_Window *win;
static SDL_Renderer *rend;
static SDL_Texture *tex;
static SDL_AudioDeviceID adev;
static SDL_GameController *pad;
static SDL_Joystick *joy;
static uint8_t pad_prev[SDL_CONTROLLER_BUTTON_MAX];
static uint8_t joy_prev[16];
static int on;
/* the framebuffer is 0xAABBGGRR; SDL's RGBA8888 wants 0xRRGGBBAA, so convert */
static uint32_t rgba[480 * 272];

static void try_open_pad(void) {
    if (pad || joy) return;
    int n = SDL_NumJoysticks();
    devlog("joysticks detectados: %d", n);
    for (int i = 0; i < n; i++) {
        if (SDL_IsGameController(i)) {
            pad = SDL_GameControllerOpen(i);
            if (pad) {
                devlog("mando: %s", SDL_GameControllerName(pad));
                return;
            }
        }
    }
    /* no game-controller mapping: fall back to the first raw joystick */
    for (int i = 0; i < n; i++) {
        joy = SDL_JoystickOpen(i);
        if (joy) {
            devlog("joystick crudo: %s (%d botones, %d ejes)", SDL_JoystickName(joy),
                   SDL_JoystickNumButtons(joy), SDL_JoystickNumAxes(joy));
            return;
        }
    }
}

void win_init(void) {
    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO | SDL_INIT_GAMECONTROLLER) != 0) {
        devlog("SDL_Init fallo: %s", SDL_GetError());
        return;
    }
    win = SDL_CreateWindow("ABISAL (PC)", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, 960, 544,
                           SDL_WINDOW_ALLOW_HIGHDPI);
    if (!win) {
        devlog("SDL_CreateWindow fallo: %s", SDL_GetError());
        return;
    }
    SDL_RaiseWindow(win);
    rend = SDL_CreateRenderer(win, -1, SDL_RENDERER_ACCELERATED);
    if (!rend) {
        rend = SDL_CreateRenderer(win, -1, SDL_RENDERER_SOFTWARE);
    }
    if (!rend) {
        devlog("SDL_CreateRenderer fallo: %s", SDL_GetError());
        return;
    }
    SDL_SetHint(SDL_HINT_RENDER_SCALE_QUALITY, "nearest"); /* crisp 2x pixels */
    tex = SDL_CreateTexture(rend, SDL_PIXELFORMAT_RGBA8888, SDL_TEXTUREACCESS_STREAMING, 480, 272);
    if (!tex) {
        devlog("SDL_CreateTexture fallo: %s", SDL_GetError());
        return;
    }
    try_open_pad();
    on = 1;
}

int win_on(void) {
    return on;
}

void win_shutdown(void) {
    if (pad) {
        SDL_GameControllerClose(pad);
        pad = NULL;
    }
    if (joy) {
        SDL_JoystickClose(joy);
        joy = NULL;
    }
    if (adev) {
        SDL_CloseAudioDevice(adev);
        adev = 0;
    }
    if (tex) {
        SDL_DestroyTexture(tex);
        tex = NULL;
    }
    if (rend) {
        SDL_DestroyRenderer(rend);
        rend = NULL;
    }
    if (win) {
        SDL_DestroyWindow(win);
        win = NULL;
    }
    SDL_Quit();
    on = 0;
}

/* one keyboard key -> the MIDP key code the game expects (see Entrada.boton) */
static int midp_code(SDL_Scancode s) {
    switch (s) {
    case SDL_SCANCODE_SPACE: return -5; /* X: nadar / interactuar / elegir */
    case SDL_SCANCODE_J: return '9';    /* cuadrado: atacar / recoger */
    case SDL_SCANCODE_K: return -7;     /* circulo: atras */
    case SDL_SCANCODE_L: return '0';    /* triangulo: PDA */
    case SDL_SCANCODE_RETURN: return -6; /* start: pausa */
    case SDL_SCANCODE_RSHIFT: return '#'; /* select */
    case SDL_SCANCODE_Q: return '1';    /* L */
    case SDL_SCANCODE_E: return '3';    /* R */
    case SDL_SCANCODE_UP: return -1;    /* cruceta arriba */
    case SDL_SCANCODE_DOWN: return -2;  /* cruceta abajo */
    case SDL_SCANCODE_LEFT: return -3;  /* cruceta izquierda */
    case SDL_SCANCODE_RIGHT: return -4; /* cruceta derecha */
    default: return 0;
    }
}

/* one gamepad button -> the MIDP key code (same codes as the keyboard) */
static int pad_code(SDL_GameControllerButton b) {
    switch (b) {
    case SDL_CONTROLLER_BUTTON_A: return -5; /* X: nadar / interactuar */
    case SDL_CONTROLLER_BUTTON_B: return -7; /* circulo: atras */
    case SDL_CONTROLLER_BUTTON_X: return '9'; /* cuadrado: atacar / recoger */
    case SDL_CONTROLLER_BUTTON_Y: return '0'; /* triangulo: PDA */
    case SDL_CONTROLLER_BUTTON_START: return -6; /* start: pausa */
    case SDL_CONTROLLER_BUTTON_BACK: return '#'; /* select */
    case SDL_CONTROLLER_BUTTON_LEFTSHOULDER: return '1'; /* L */
    case SDL_CONTROLLER_BUTTON_RIGHTSHOULDER: return '3'; /* R */
    case SDL_CONTROLLER_BUTTON_DPAD_UP: return -1;
    case SDL_CONTROLLER_BUTTON_DPAD_DOWN: return -2;
    case SDL_CONTROLLER_BUTTON_DPAD_LEFT: return -3;
    case SDL_CONTROLLER_BUTTON_DPAD_RIGHT: return -4;
    default: return 0;
    }
}

/* raw joystick buttons (common layout) -> MIDP key codes */
static int joy_code(int b) {
    switch (b) {
    case 0: return -5;   /* A / cross */
    case 1: return -7;   /* B / circle */
    case 2: return '9';  /* X / square */
    case 3: return '0';  /* Y / triangle */
    case 4: return '1';  /* LB / L */
    case 5: return '3';  /* RB / R */
    case 6: return '#';  /* back / select */
    case 7: return -6;   /* start */
    case 8: return -3;   /* dpad left */
    case 9: return -1;   /* dpad up */
    case 10: return -4;  /* dpad right */
    case 11: return -2;  /* dpad down */
    default: return 0;
    }
}

static int axis255(Sint16 v) {
    int a = 128 + v / 256;
    return a < 0 ? 0 : (a > 255 ? 255 : a);
}

/* 0xAABBGGRR -> 0xRRGGBBAA (a straight byte reversal) */
static void convert(const uint32_t *pix) {
    for (int i = 0; i < 480 * 272; i++) {
        uint32_t c = pix[i];
        rgba[i] = ((c & 0xff) << 24) | ((c & 0xff00) << 8) | ((c >> 8) & 0xff00) | ((c >> 24) & 0xff);
    }
}

static int calls;

void win_present(const uint32_t *pix) {
    if (!on) {
        return;
    }
    calls++;
    if (calls == 30) {
        int nb = 0;
        for (int i = 0; i < 480 * 272; i++) {
            if ((pix[i] & 0xffffffu) != 0) nb++;
        }
        devlog("win_present #30: %d pixeles no negros de %d", nb, 480 * 272);
    }
    convert(pix);
    SDL_UpdateTexture(tex, NULL, rgba, 480 * 4);
    SDL_RenderClear(rend);
    SDL_RenderCopy(rend, tex, NULL, NULL);
    SDL_RenderPresent(rend);

    SDL_Event e;
    while (SDL_PollEvent(&e)) {
        if (e.type == SDL_QUIT) {
            plat_push_event(4 << 24); /* type 4 = request exit */
        } else if (e.type == SDL_KEYDOWN && !e.key.repeat) {
            int c = midp_code(e.key.keysym.scancode);
            if (c) {
                plat_push_event((1 << 24) | (c & 0xffff));
            }
        } else if (e.type == SDL_KEYUP) {
            int c = midp_code(e.key.keysym.scancode);
            if (c) {
                plat_push_event((2 << 24) | (c & 0xffff));
            }
        } else if (e.type == SDL_CONTROLLERDEVICEADDED) {
            if (!pad) pad = SDL_GameControllerOpen(e.cdevice.which);
        } else if (e.type == SDL_CONTROLLERDEVICEREMOVED) {
            if (pad && SDL_JoystickInstanceID(SDL_GameControllerGetJoystick(pad)) == e.cdevice.which) {
                SDL_GameControllerClose(pad);
                pad = NULL;
            }
        }
    }

    /* ---- analog stick: the gamepad if present, else WASD ---- */
    int ax = 128, ay = 128;
    if (pad) {
        ax = axis255(SDL_GameControllerGetAxis(pad, SDL_CONTROLLER_AXIS_LEFTX));
        ay = axis255(SDL_GameControllerGetAxis(pad, SDL_CONTROLLER_AXIS_LEFTY));
        /* buttons: edge-detect so the game sees press / release */
        for (int b = 0; b < SDL_CONTROLLER_BUTTON_MAX; b++) {
            Uint8 now = SDL_GameControllerGetButton(pad, (SDL_GameControllerButton)b);
            if (now != pad_prev[b]) {
                pad_prev[b] = now;
                int c = pad_code((SDL_GameControllerButton)b);
                if (c) {
                    plat_push_event(((now ? 1 : 2) << 24) | (c & 0xffff));
                }
            }
        }
    } else if (joy) {
        int nb = SDL_JoystickNumButtons(joy);
        for (int b = 0; b < nb && b < 16; b++) {
            Uint8 now = SDL_JoystickGetButton(joy, b);
            if (now != joy_prev[b]) {
                joy_prev[b] = now;
                int c = joy_code(b);
                if (c) {
                    plat_push_event(((now ? 1 : 2) << 24) | (c & 0xffff));
                }
            }
        }
        ax = axis255(SDL_JoystickGetAxis(joy, 0));
        ay = axis255(SDL_JoystickGetAxis(joy, 1));
    } else {
        const uint8_t *k = SDL_GetKeyboardState(NULL);
        if (k[SDL_SCANCODE_A]) ax = 0;
        if (k[SDL_SCANCODE_D]) ax = 255;
        if (k[SDL_SCANCODE_W]) ay = 0;   /* stick up = look up */
        if (k[SDL_SCANCODE_S]) ay = 255; /* stick down = look down */
    }
    plat_set_analog((ax << 8) | ay);
}

/* ---- SDL audio: play the mixer's output while the window is open ---- */

int win_audio_init(void) {
    if (!on) return 0;
    SDL_AudioSpec want, have;
    SDL_zero(want);
    want.freq = 44100;
    want.format = AUDIO_S16SYS;
    want.channels = 2;
    want.samples = 256; /* small buffer: low latency */
    adev = SDL_OpenAudioDevice(NULL, 0, &want, &have, 0);
    if (!adev) {
        devlog("SDL audio fallo: %s", SDL_GetError());
        return 0;
    }
    SDL_PauseAudioDevice(adev, 0);
    devlog("audio SDL abierto (%d Hz, %d canales)", have.freq, have.channels);
    return 1;
}

void win_audio_feed(const int16_t *buf, int frames) {
    if (!adev) return;
    /* keep the queue short: skip a chunk instead of letting latency build up */
    if (SDL_GetQueuedAudioSize(adev) > 44100u * 2u * 2u * 15u / 100u) {
        return;
    }
    SDL_QueueAudio(adev, buf, (uint32_t)frames * 2 * sizeof(int16_t));
}
