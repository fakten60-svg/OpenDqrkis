#!/usr/bin/env bash
#
# Headless load test for OpenDqrkis.
#
# Launches the Fabric client in a virtual X server (Xvfb), waits until the client
# has built its font/texture atlases (i.e. is drawing the title screen), lets
# Minecraft write its own screenshot via the in-game F2 key, and then shuts the
# client down again.
#
# Minecraft's own screenshot goes through glReadPixels, so it works even when
# Xvfb/xwd cannot see the GLX surface.
#
# Usage:
#   xvfb-run -a -s "-screen 0 1280x720x24" bash scripts/capture-client-load.sh
#
# Requires: xvfb, xdotool, x11-utils (xwininfo), netpbm (optional)
set -u

cd "$(dirname "$0")/.." || exit 1

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"

rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null

# Skip the vanilla first-run accessibility onboarding screen so the client lands
# directly on the main menu. This is a disposable runtime file in the ignored run/ dir.
mkdir -p run
printf 'onboardAccessibility:false\n' > run/options.txt

rm -rf run/logs run/screenshots
./gradlew runClient --no-daemon --console=plain > /tmp/grun-capture.log 2>&1 &
GRADLE_PID=$!

READY=no
for i in $(seq 1 110); do
	if grep -q "atlas/gui.png-atlas" run/logs/latest.log 2>/dev/null; then
		echo "client built its atlases after ~${i}s"
		READY=yes
		break
	fi
	sleep 1
done

if [ "$READY" = "no" ]; then
	echo "client did not reach the title screen in time; tail of log:"
	tail -30 /tmp/grun-capture.log 2>/dev/null
fi

# Let the render loop settle on the title screen.
FIRST_WAIT="${FIRST_WAIT:-20}"
SECOND_WAIT="${SECOND_WAIT:-20}"
sleep "$FIRST_WAIT"

echo "--- X window tree ---"
xwininfo -root -tree 2>/dev/null | sed -n '1,25p'

# Drive Minecraft's own screenshot key (F2) through XTEST. Minecraft writes the
# framebuffer through glReadPixels, which works even when Xvfb cannot show GLX.
# Move the pointer over the window so XTEST key events reach it, but do not click:
# clicking would navigate away from the main menu.
if command -v xdotool >/dev/null 2>&1; then
	xdotool mousemove 640 360 >/dev/null 2>&1
	sleep 2
	xdotool key F2 >/dev/null 2>&1
	echo "sent F2 (screenshot 1)"
	sleep "$SECOND_WAIT"
	xdotool key F2 >/dev/null 2>&1
	echo "sent F2 (screenshot 2)"
	sleep 6
fi

echo "--- Minecraft screenshots ---"
ls -la run/screenshots/ 2>/dev/null || echo "(no screenshots directory)"

# Fallback: raw X capture.
if command -v xwd >/dev/null 2>&1 && command -v pnmtopng >/dev/null 2>&1; then
	xwd -root -silent > /tmp/menu.xwd 2>/dev/null
	xwdtopnm /tmp/menu.xwd 2>/dev/null | pnmtopng > /tmp/menu-root.png 2>/dev/null
	echo "root capture: /tmp/menu-root.png ($(stat -c%s /tmp/menu-root.png 2>/dev/null || echo 0) bytes)"
fi

# Shut the client down again.
kill "$GRADLE_PID" 2>/dev/null
sleep 1
pkill -f devlaunchinjector 2>/dev/null
pkill -f KnotClient 2>/dev/null
sleep 2

LEFT=$(pgrep -c -f "devlaunchinjector|KnotClient" 2>/dev/null || true)
echo "remaining client processes: ${LEFT:-0}"
exit 0
