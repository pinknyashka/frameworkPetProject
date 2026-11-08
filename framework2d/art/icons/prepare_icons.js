const fs = require("fs");
const path = require("path");

let sharp;
try {
  sharp = require("sharp");
} catch (e) {
  console.error("sharp is required: npm install sharp, or run with NODE_PATH pointing at a node_modules that has it");
  process.exit(1);
}

const OUT_SIZE = 256;
const SRC_DIR = path.join(__dirname, "src");
const OUT_DIR = path.resolve(__dirname, "..", "..", "assets", "Images");

const SOURCES = {
  Infantry: "infantry",
  Cavalry: "cavalry",
  Minigun: "minigun",
};

const BG_TOLERANCE = 72;
const STEEL_BIAS = [0.86, 0.98, 1.16];

function bgDistance(data, i, bg) {
  return Math.abs(data[i] - bg[0]) + Math.abs(data[i + 1] - bg[1]) + Math.abs(data[i + 2] - bg[2]);
}

function cornerBackground(data, info) {
  const spots = [
    [0, 0],
    [info.width - 2, 0],
    [0, info.height - 2],
    [info.width - 2, info.height - 2],
  ];
  const sum = [0, 0, 0];
  for (const [x, y] of spots) {
    const i = (y * info.width + x) * info.channels;
    sum[0] += data[i];
    sum[1] += data[i + 1];
    sum[2] += data[i + 2];
  }
  return sum.map((v) => v / spots.length);
}

function floodOutside(data, info, bg) {
  const w = info.width;
  const h = info.height;
  const outside = new Uint8Array(w * h);
  const stack = [];
  const isBg = (p) => bgDistance(data, p * info.channels, bg) < BG_TOLERANCE;
  const seed = (p) => {
    if (!outside[p] && isBg(p)) {
      outside[p] = 1;
      stack.push(p);
    }
  };

  for (let x = 0; x < w; x++) {
    seed(x);
    seed((h - 1) * w + x);
  }
  for (let y = 0; y < h; y++) {
    seed(y * w);
    seed(y * w + w - 1);
  }

  while (stack.length) {
    const p = stack.pop();
    const x = p % w;
    if (x > 0 && !outside[p - 1] && isBg(p - 1)) {
      outside[p - 1] = 1;
      stack.push(p - 1);
    }
    if (x < w - 1 && !outside[p + 1] && isBg(p + 1)) {
      outside[p + 1] = 1;
      stack.push(p + 1);
    }
    if (p - w >= 0 && !outside[p - w] && isBg(p - w)) {
      outside[p - w] = 1;
      stack.push(p - w);
    }
    if (p + w < w * h && !outside[p + w] && isBg(p + w)) {
      outside[p + w] = 1;
      stack.push(p + w);
    }
  }

  return outside;
}

function bounds(outside, info) {
  let minX = info.width;
  let minY = info.height;
  let maxX = 0;
  let maxY = 0;
  for (let y = 0; y < info.height; y++) {
    for (let x = 0; x < info.width; x++) {
      if (!outside[y * info.width + x]) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }
  }
  let side = Math.max(maxX - minX + 1, maxY - minY + 1) + 2;
  const cx = (minX + maxX) / 2;
  const cy = (minY + maxY) / 2;
  const x0 = Math.max(0, Math.round(cx - side / 2));
  const y0 = Math.max(0, Math.round(cy - side / 2));
  if (x0 + side > info.width || y0 + side > info.height) {
    side = Math.min(info.width, info.height) - 2;
  }
  return { x0, y0, side };
}

function cut(data, info, region, outside) {
  const out = Buffer.alloc(region.side * region.side * 4);
  for (let y = 0; y < region.side; y++) {
    for (let x = 0; x < region.side; x++) {
      const s = ((y + region.y0) * info.width + (x + region.x0)) * info.channels;
      const d = (y * region.side + x) * 4;
      out[d] = data[s];
      out[d + 1] = data[s + 1];
      out[d + 2] = data[s + 2];
      out[d + 3] = outside[(y + region.y0) * info.width + (x + region.x0)] ? 0 : 255;
    }
  }
  return out;
}

function featherAlpha(buf, side) {
  const tmp = Buffer.from(buf);
  for (let y = 1; y < side - 1; y++) {
    for (let x = 1; x < side - 1; x++) {
      const p = (y * side + x) * 4 + 3;
      buf[p] = (tmp[p] + tmp[p - 4] + tmp[p + 4]) / 3;
    }
  }
  for (let y = 1; y < side - 1; y++) {
    for (let x = 1; x < side - 1; x++) {
      const p = (y * side + x) * 4 + 3;
      buf[p] = (tmp[p] + tmp[p - side * 4] + tmp[p + side * 4]) / 3;
    }
  }
  return buf;
}

function steelVariant(buf) {
  for (let i = 0; i < buf.length; i += 4) {
    const luma = 0.299 * buf[i] + 0.587 * buf[i + 1] + 0.114 * buf[i + 2];
    const inverted = 255 - luma;
    buf[i] = Math.min(255, inverted * STEEL_BIAS[0]);
    buf[i + 1] = Math.min(255, inverted * STEEL_BIAS[1]);
    buf[i + 2] = Math.min(255, inverted * STEEL_BIAS[2]);
  }
  return buf;
}

async function emit(name, buf, side) {
  const target = path.join(OUT_DIR, name);
  await sharp(buf, { raw: { width: side, height: side, channels: 4 } })
    .resize(OUT_SIZE, OUT_SIZE, { fit: "fill", kernel: "lanczos3" })
    .png()
    .toFile(target);
  console.log("wrote", path.relative(path.resolve(__dirname, "..", ".."), target));
}

async function process(source, base) {
  const img = sharp(path.join(SRC_DIR, source + ".png")).ensureAlpha();
  const { data, info } = await img.raw().toBuffer({ resolveWithObject: true });

  const bg = cornerBackground(data, info);
  const outside = floodOutside(data, info, bg);
  const region = bounds(outside, info);

  const light = featherAlpha(cut(data, info, region, outside), region.side);
  const dark = featherAlpha(steelVariant(cut(data, info, region, outside)), region.side);

  console.log(source, "bg", bg.map((v) => Math.round(v)).join("/"), "disc", region.side + "px");
  await emit(base + "_light.png", light, region.side);
  await emit(base + "_dark.png", dark, region.side);
}

async function main() {
  for (const [source, base] of Object.entries(SOURCES)) {
    await process(source, base);
  }
}

main().catch((e) => {
  console.error(e.stack || e.message);
  process.exit(1);
});
