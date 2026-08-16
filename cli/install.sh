#!/usr/bin/env bash
# Installs the `krypton` CLI into /usr/local/bin (symlink).
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
TARGET="$HERE/krypton"
DEST="/usr/local/bin/krypton"

if [ ! -x "$TARGET" ]; then
  chmod +x "$TARGET"
fi

if [ "$(uname -s)" != "Darwin" ] && [ -d "$HOME/.local/bin" ]; then
  DEST="$HOME/.local/bin/krypton"
  mkdir -p "$HOME/.local/bin"
fi

if ! ln -sf "$TARGET" "$DEST" 2>/dev/null; then
  DEST="$HOME/.local/bin/krypton"
  mkdir -p "$HOME/.local/bin"
  ln -sf "$TARGET" "$DEST"
  echo "Note: /usr/local/bin not writable, installed to $DEST instead."
  case ":$PATH:" in
    *":$HOME/.local/bin:"*) ;;
    *) echo "Add to your shell: export PATH=\"\$HOME/.local/bin:\$PATH\"" ;;
  esac
fi
echo "krypton CLI installed -> $DEST"
"$DEST" version