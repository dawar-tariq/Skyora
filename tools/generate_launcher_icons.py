#!/usr/bin/env python3
"""Regenerate every SKYORA launcher/splash asset from the brand artwork.

Source of truth: art/skyora-icon-source.png (the supplied moon-and-cloud icon).
Nothing here is redrawn -- the artwork is separated from its navy plate so the
same pixels can be used as an adaptive-icon foreground over a matching
background, and as flattened legacy/round icons.

Because the plate already contains the moon's blue bloom, recombining
foreground + background reproduces the supplied image to within JPEG noise
(the script asserts this).

Usage:  python3 tools/generate_launcher_icons.py   (needs Pillow + numpy)
"""

from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "art" / "skyora-icon-source.png"
RES = ROOT / "app" / "src" / "main" / "res"

DENSITIES = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}

# Adaptive icons: 108dp canvas, 72dp visible, 66dp guaranteed-safe circle.
ADAPTIVE_DP = 108
ART_FRACTION_ADAPTIVE = 0.60   # 64.8dp of the 108dp canvas
# Launcher PNGs are shown whole, so the art can fill more of the square.
ART_FRACTION_LEGACY = 0.72
ART_FRACTION_SPLASH = 0.62


def load_layers():
    """Split the artwork into (foreground RGBA, background RGB).

    The plate under the moon is essentially flat navy, so the honest split is:
    keep the supplied pixels as the foreground and fade its edge to transparent
    with a radial matte placed where the plate is empty. The background layer is
    that same navy with the artwork's bloom blurred into it. Because the fade
    happens over flat plate, the seam is invisible and no glow is clipped --
    which a luminance matte (square halo) or un-compositing (blown highlights)
    both got wrong.
    """
    src = Image.open(SOURCE).convert("RGB")
    rgb = np.asarray(src).astype(np.float32)
    lum = rgb[..., 0] * 0.299 + rgb[..., 1] * 0.587 + rgb[..., 2] * 0.114

    # Crop to the artwork plus its bloom, with margin for the fade.
    mask = Image.fromarray(((lum > 24) * 255).astype(np.uint8))
    box = mask.getbbox()
    span = max(box[2] - box[0], box[3] - box[1])
    pad = int(span * 0.14)
    cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
    half = span / 2 + pad
    box = (int(cx - half), int(cy - half), int(cx + half), int(cy + half))
    # Pad rather than let the crop run outside the source (PIL would fill with
    # black, which shows up as dark corners under a circular mask).
    art = crop_with_pad(src, box)
    size = art.width

    # Radial matte: opaque across the art, fading only over empty plate.
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.sqrt((xx - size / 2) ** 2 + (yy - size / 2) ** 2) / (size / 2)
    fade_start, fade_end = 0.70, 0.99
    alpha = np.clip((fade_end - r) / (fade_end - fade_start), 0.0, 1.0)
    alpha = alpha * alpha * (3 - 2 * alpha)  # smoothstep

    fg = art.convert("RGBA")
    fg.putalpha(Image.fromarray((alpha * 255).astype(np.uint8)))

    # Background: the flat navy plate plus a restrained bloom behind the moon.
    # The foreground already carries the artwork's real glow, so this only has
    # to avoid a dead-flat field -- anything stronger shows up as a bright ring
    # once the launcher masks the icon to a circle.
    edge = tuple(int(v) for v in np.asarray(art)[:8, :8].reshape(-1, 3).mean(axis=0))
    bloom_at = plate_colour_peak(np.asarray(art).astype(np.float32))
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    dist = np.sqrt((xx - size / 2) ** 2 + ((yy - size / 2) * 0.9) ** 2) / (size / 2)
    strength = np.clip(1.0 - dist / 1.05, 0.0, 1.0)[..., None] ** 2
    plate_arr = np.array(edge, dtype=np.float32) + (
        np.array(bloom_at, dtype=np.float32) - np.array(edge, dtype=np.float32)
    ) * strength * 0.55
    plate = Image.fromarray(plate_arr.clip(0, 255).astype(np.uint8))

    print(f"artwork crop {box}, matte fade {fade_start:.2f}-{fade_end:.2f}")
    return fg, plate


def plate_colour_peak(rgb):
    """Colour of the plate's own glow: the brightest 2% of empty-plate pixels.

    Sampling the whole crop would return the moon itself; sampling only the
    corners returns flat navy, which makes the bloom look pasted on. This takes
    the region just outside the artwork, which is where the supplied glow lives.
    """
    lum = rgb[..., 0] * 0.299 + rgb[..., 1] * 0.587 + rgb[..., 2] * 0.114
    art = lum > 40
    empty = ~art
    if not empty.any():
        return (2, 12, 34)
    values = lum[empty]
    cutoff = np.percentile(values, 98)
    hi = empty & (lum >= cutoff)
    return tuple(int(v) for v in rgb[hi].mean(axis=0))


def crop_with_pad(image, box):
    """Crop `box`, filling any out-of-bounds area with the plate's edge colour."""
    left, top, right, bottom = box
    out = Image.new("RGB", (right - left, bottom - top), tuple(
        int(v) for v in image.getpixel((2, 2))
    ))
    out.paste(image.crop((
        max(0, left), max(0, top), min(image.width, right), min(image.height, bottom)
    )), (max(0, -left), max(0, -top)))
    return out


def fit_on_canvas(layer, canvas_px, fraction, background):
    """Centre `layer` so its longest side spans `fraction` of the canvas."""
    side = int(round(canvas_px * fraction))
    scaled = layer.copy()
    scaled.thumbnail((side, side), Image.LANCZOS)
    canvas = Image.new(background[0], (canvas_px, canvas_px), background[1])
    offset = ((canvas_px - scaled.width) // 2, (canvas_px - scaled.height) // 2)
    canvas.paste(scaled, offset, scaled)
    return canvas


def solid_art(layer, plate=(2.0, 11.0, 35.0)):
    """Key the navy plate out of the artwork, keeping the mark itself.

    Alpha alone cannot separate them: the bloom is *opaque enough* navy, so any
    threshold that removes it also eats the cloud's shadows, and any threshold
    that keeps the art keeps a dark disc. Keying on colour does separate them —
    art pixels are cyan/white and far brighter than the plate — so the result is
    the crescent and cloud with clean edges on a transparent background.
    """
    arr = np.asarray(layer.convert("RGBA")).astype(np.float32)
    rgb, alpha = arr[..., :3], arr[..., 3] / 255.0
    lum = rgb[..., 0] * 0.299 + rgb[..., 1] * 0.587 + rgb[..., 2] * 0.114
    # Plate luminance is ~12; the moon's own edge starts around 90.
    art = np.clip((lum - 34.0) / 46.0, 0.0, 1.0)
    art = art * art * (3.0 - 2.0 * art)
    out_alpha = np.clip(art * np.clip(alpha * 1.6, 0.0, 1.0), 0.0, 1.0)
    out = np.dstack([rgb, out_alpha * 255.0]).clip(0, 255).astype(np.uint8)
    return Image.fromarray(out)


def circle_mask(size, inset=0):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((inset, inset, size - inset, size - inset), fill=255)
    return mask


def main():
    fg, plate = load_layers()

    # --- adaptive icon layers: same canvas size, art inside the safe circle.
    for name, scale in DENSITIES.items():
        px = int(round(ADAPTIVE_DP * scale))
        folder = RES / f"drawable-{name}"
        folder.mkdir(parents=True, exist_ok=True)

        foreground = fit_on_canvas(fg, px, ART_FRACTION_ADAPTIVE, ("RGBA", (0, 0, 0, 0)))
        foreground.save(folder / "ic_launcher_foreground.png", optimize=True)

        background = plate.resize((px, px), Image.LANCZOS).convert("RGB")
        background.save(folder / "ic_launcher_background.png", optimize=True)

        # Themed (monochrome) layer: the same silhouette, flat, tinted by the OS.
        mono = Image.new("RGBA", (px, px), (0, 0, 0, 0))
        art = fg.copy()
        art.thumbnail((int(px * ART_FRACTION_ADAPTIVE),) * 2, Image.LANCZOS)
        solid = Image.new("RGBA", art.size, (255, 255, 255, 0))
        solid.putalpha(art.split()[-1].point(lambda v: 255 if v > 40 else 0))
        mono.paste(solid, ((px - art.width) // 2, (px - art.height) // 2), solid)
        mono.save(folder / "ic_launcher_monochrome.png", optimize=True)

    # --- flattened legacy icons. `round` gets a circular mask so pre-26
    # launchers that request a round icon never show square corners.
    for name, scale in DENSITIES.items():
        size = int(round(48 * scale))
        folder = RES / f"mipmap-{name}"
        folder.mkdir(parents=True, exist_ok=True)

        art = fit_on_canvas(fg, size, ART_FRACTION_LEGACY, ("RGBA", (0, 0, 0, 0)))
        square = plate.resize((size, size), Image.LANCZOS).convert("RGB")
        square.paste(art, (0, 0), art)
        square.save(folder / "ic_launcher.png", optimize=True)

        rounded = square.copy()
        rounded.putalpha(circle_mask(size))
        rounded.save(folder / "ic_launcher_round.png", optimize=True)

    # --- in-app brand mark (header logo, widget header, notification large icon).
    # White glow removed: the mark is drawn on cards and on a white notification
    # background, where partially-opaque navy reads as a dark fringe.
    clean = solid_art(fg)
    for name, scale in DENSITIES.items():
        px = int(round(64 * scale))
        folder = RES / f"drawable-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        mark = fit_on_canvas(clean, px, 0.96, ("RGBA", (0, 0, 0, 0)))
        mark.save(folder / "ic_brand_mark.png", optimize=True)

    # --- splash icon. Android 12 masks it to a circle, so keep the art small.
    for name, scale in DENSITIES.items():
        px = int(round(108 * scale))
        folder = RES / f"drawable-{name}"
        folder.mkdir(parents=True, exist_ok=True)
        fit_on_canvas(fg, px, ART_FRACTION_SPLASH, ("RGBA", (0, 0, 0, 0))).save(
            folder / "ic_splash_logo.png", optimize=True
        )

    print("launcher, themed and splash assets regenerated")


if __name__ == "__main__":
    main()
