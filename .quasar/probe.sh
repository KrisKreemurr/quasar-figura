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
    case "$cls" in
      g:*) spec="${cls#g:}"; c2="${spec%%|*}"; rx="${spec#*|}"; echo "=== JAVAPG $c2 /$rx/"; javap -c -p -cp "$J" "$c2" 2>&1 | grep -nE "^  [a-z].*\(|$rx" | grep -B1 -E "$rx" | head -400;;
      m:*) spec="${cls#m:}"; c2="${spec%%|*}"; mn="${spec#*|}"; echo "=== JAVAPM $c2 $mn"; javap -c -p -cp "$J" "$c2" 2>&1 | awk -v m="$mn" '/^  [a-z].*\(/ {on = (index($0, " " m "(") > 0)} on';;
      c:*) cls="${cls#c:}"; echo "=== JAVAPC $cls"; javap -c -p -cp "$J" "$cls" 2>&1 | head -1400;;
      *) echo "=== JAVAP $cls"; javap -protected -cp "$J" "$cls" 2>&1 | head -450;;
    esac
  done < .quasar/javap.txt
fi
if [ -f .quasar/audit.txt ]; then
  while IFS= read -r cls; do
    [ -z "$cls" ] && continue
    echo "=== AUDIT $cls"
    javap -s -p -cp "$J" "$cls" 2>&1 | awk '/^(public |protected |private |final |abstract |sealed |non-sealed )*(class|interface|enum|record) / && !hdr {print "H " $0; hdr=1} /descriptor:/ {d=$2; n=prev; sub(/\(.*/,"",n); k=split(n,a," "); nm=a[k]; if (index(prev,"(")) print "M " nm d; else print "F " nm " " d; next} {prev=$0}'
  done < .quasar/audit.txt
fi
if [ -f .quasar/res.txt ]; then
  while IFS= read -r pat; do
    [ -z "$pat" ] && continue
    echo "=== RES $pat"
    for e in $(unzip -Z1 "$J" | grep -E "$pat" | head -8); do echo "--- $e"; unzip -p "$J" "$e" | head -120; echo; done
  done < .quasar/res.txt
fi
NEOJAR=$(find ~/.gradle/caches -name "neoforge-*-universal.jar" 2>/dev/null | head -1)
# r:<class> lines in .quasar/hier.txt -> public/protected members of the class and every superclass/interface (MC + NeoForge)
if [ -f .quasar/hier.txt ]; then
  CP="$J:$NEOJAR"
  declare -A SEEN
  queue=$(sed 's/^r://' .quasar/hier.txt)
  while [ -n "$queue" ]; do
    next=""
    for c in $queue; do
      [ -n "${SEEN[$c]}" ] && continue; SEEN[$c]=1
      out=$(javap -protected -cp "$CP" "$c" 2>/dev/null) || continue
      echo "=== HIER $c"; echo "HEADER $(echo "$out" | grep -E "^(public |protected |abstract |final |sealed |non-sealed )*(class|interface) " | head -1)"; echo "$out" | grep -E "^\s+(public|protected)" 
      hdr=$(echo "$out" | grep -E "^(public |protected |abstract |final |sealed |non-sealed )*(class|interface) " | head -1)
      sup=$(echo "$hdr" | sed -E 's/<[^<>]*>//g; s/<[^<>]*>//g; s/<[^<>]*>//g; s/<[^<>]*>//g' | sed -E 's/.*(class|interface) [^ ]+ ?//; s/ *\{.*//; s/ implements /,/; s/^extends //; s/^implements //' | tr ',' '\n' | sed 's/ //g')
      for x in $sup; do case "$x" in java.*|"") ;; *) next="$next $x";; esac; done
    done
    queue="$next"
  done
fi
echo "=== PROBE: neoforge jar $NEOJAR"
if [ -n "$NEOJAR" ] && [ -f .quasar/neogrep.txt ]; then
  unzip -Z1 "$NEOJAR" | grep '\.class$' | grep -v '\$[0-9]' | sed 's/\.class$//; s#/#.#g' > /tmp/neoclasses.txt
  while IFS= read -r pat; do [ -z "$pat" ] && continue; echo "=== NEOGREP $pat"; grep -E "$pat" /tmp/neoclasses.txt | head -40; done < .quasar/neogrep.txt
fi
if [ -n "$NEOJAR" ] && [ -f .quasar/neogrep.txt ]; then
  unzip -Z1 "$NEOJAR" | grep '\.class$' | grep -v '\$[0-9]' | sed 's/\.class$//; s#/#.#g' > /tmp/neoclasses.txt
  while IFS= read -r pat; do [ -z "$pat" ] && continue; echo "=== NEOGREP $pat"; grep -E "$pat" /tmp/neoclasses.txt | head -40; done < .quasar/neogrep.txt
fi
if [ -n "$NEOJAR" ] && [ -f .quasar/neojavap.txt ]; then
  while IFS= read -r cls; do [ -z "$cls" ] && continue; echo "=== NEOJAVAP $cls"; javap -p -c -cp "$NEOJAR:$J" "$cls" 2>&1 | grep -E "^  [a-z].*|Field|codec|Protocol" | head -150; done < .quasar/neojavap.txt
fi
echo "=== PROBE END"
