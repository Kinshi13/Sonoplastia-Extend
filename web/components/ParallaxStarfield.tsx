"use client";

import { useEffect, useRef } from "react";

type Star = { xFraction: number; yFraction: number; radius: number; depth: number };

/**
 * Fase 9 (Master Plan): the web mirror of the Android starfield (ParallaxStarfield.kt, Fase 7) -
 * same idea, different input. There is no swipeable carousel on the site to drive it from, so
 * this reads the page's own scroll position instead: farther/dimmer stars (low depth) barely
 * move, closer/brighter ones drift more, giving a real depth cue rather than a moving wallpaper.
 *
 * Fully scroll-driven (a single passive `scroll` listener, removed on unmount) - no `setInterval`,
 * no polling, nothing that outlives this component, consistent with the battery lesson from the
 * old Android background-music bug (Fase 6). Respects `prefers-reduced-motion`: stars still render,
 * they just don't drift.
 */
export function ParallaxStarfield({ starCount = 44 }: { starCount?: number }) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const starsRef = useRef<Star[]>([]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Fixed seed so the sky doesn't reshuffle on every render/scroll tick.
    let seed = 4242;
    const random = () => {
      seed = (seed * 1103515245 + 12345) & 0x7fffffff;
      return (seed % 10000) / 10000;
    };
    if (starsRef.current.length === 0) {
      starsRef.current = Array.from({ length: starCount }, () => ({
        xFraction: random(),
        yFraction: random(),
        radius: random() * 1.4 + 0.5,
        depth: random() * 0.8 + 0.2,
      }));
    }

    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const isDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
    const starColor = isDark ? "243,244,250" : "92,98,128";

    let width = 0;
    let height = 0;
    const resize = () => {
      width = window.innerWidth;
      height = window.innerHeight;
      canvas.style.width = `${width}px`;
      canvas.style.height = `${height}px`;
      canvas.width = width * window.devicePixelRatio;
      canvas.height = height * window.devicePixelRatio;
      ctx.setTransform(window.devicePixelRatio, 0, 0, window.devicePixelRatio, 0, 0);
      draw();
    };

    function draw() {
      if (!ctx) return;
      ctx.clearRect(0, 0, width, height);
      const scrollDrift = reduceMotion ? 0 : (window.scrollY % (height || 1)) * 0.06;
      for (const star of starsRef.current) {
        const y = (star.yFraction * height - scrollDrift * star.depth + height) % height;
        ctx.beginPath();
        ctx.fillStyle = `rgba(${starColor}, ${0.08 + star.depth * 0.1})`;
        ctx.arc(star.xFraction * width, y, star.radius, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    let ticking = false;
    const onScroll = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(() => {
        draw();
        ticking = false;
      });
    };

    resize();
    window.addEventListener("resize", resize);
    if (!reduceMotion) window.addEventListener("scroll", onScroll, { passive: true });

    return () => {
      window.removeEventListener("resize", resize);
      window.removeEventListener("scroll", onScroll);
    };
  }, [starCount]);

  return (
    <canvas
      ref={canvasRef}
      aria-hidden="true"
      className="pointer-events-none fixed inset-0 -z-10"
    />
  );
}
