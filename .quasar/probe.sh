#!/bin/bash
# Quasar MC porting helper: prints Minecraft 26.3 class names / signatures into the CI log.
# Driven by .quasar/grep.txt (regex per line -> matching class names)
# and .quasar/javap.txt (fully qualified class names -> public/protected signatures).
cd "$(dirname "$0")/.."
echo "=== PROBE: artifacts"; ls build/moddev/artifacts 2>/dev/null
J=$(ls build/moddev/artifacts/*.jar 2>/dev/null | grep -v -- '-sources' | grep -i neoforge | head -1)
[ -z "$J" ] && J=$(ls build/moddev/artifacts/*.jar 2>/dev/null | grep -v -- '-sources' | head -1)
echo "=== PROBE: jar $J"
[ -z "$J" ] && exit 0
unzip -Z1 "$J" | grep '\.class$' | grep -v '\$[0-9]' | sed 's/\.class$//; s#/#.#g' > /tmp/classes.txt
echo "=== PROBE: $(wc -l < /tmp/classes.txt) classes"
if [ -f .quasar/dump-classes ]; then echo "=== CLASSLIST BEGIN"; cat /tmp/classes.txt; echo "=== CLASSLIST END"; fi
if [ -f .quasar/grep.txt ]; then
  while IFS= read -r pat; do
    [ -z "$pat" ] && continue
    echo "=== GREP $pat"
    grep -E "$pat" /tmp/classes.txt | head -60
  done < .quasar/grep.txt
fi
if [ -f .quasar/javap.txt ]; then
  while IFS= read -r cls; do
    [ -z "$cls" ] && continue
    echo "=== JAVAP $cls"
    javap -protected -cp "$J" "$cls" 2>&1 | head -150
  done < .quasar/javap.txt
fi
NEOJAR=$(find ~/.gradle/caches -name "neoforge-*-universal.jar" 2>/dev/null | head -1)
echo "=== PROBE: neoforge jar $NEOJAR"
if [ -n "$NEOJAR" ] && [ -f .quasar/neogrep.txt ]; then
  unzip -Z1 "$NEOJAR" | grep '\.class$' | grep -v '\$[0-9]' | sed 's/\.class$//; s#/#.#g' > /tmp/neoclasses.txt
  while IFS= read -r pat; do [ -z "$pat" ] && continue; echo "=== NEOGREP $pat"; grep -E "$pat" /tmp/neoclasses.txt | head -40; done < .quasar/neogrep.txt
fi
if [ -n "$NEOJAR" ] && [ -f .quasar/neojavap.txt ]; then
  while IFS= read -r cls; do [ -z "$cls" ] && continue; echo "=== NEOJAVAP $cls"; javap -protected -cp "$NEOJAR:$J" "$cls" 2>&1 | head -80; done < .quasar/neojavap.txt
fi
echo "=== PROBE END"
