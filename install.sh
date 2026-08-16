#!/usr/bin/env bash
# Krypton Client installer
#
#   curl -fsSL https://raw.githubusercontent.com/<OWNER>/<REPO>/main/install.sh | bash
#
set -e

REPO_URL="${KRYP_REPO_URL:-https://github.com/xiaoling1231231231-glitch/krypton-client.git}"
BRANCH="${KRYP_BRANCH:-main}"
INSTALL_DIR="${KRYP_INSTALL_DIR:-$HOME/KryptonClient}"
BIN_DIR="${KRYP_BIN_DIR:-$HOME/.local/bin}"

CYAN='\033[0;36m'; GREEN='\033[0;32m'; RED='\033[0;31m'; BOLD='\033[1m'; DIM='\033[2m'; RESET='\033[0m'
say() { printf "${BOLD}Krypton${RESET} %s\n" "$1"; }

command -v git >/dev/null || { echo "${RED}error: git is required${RESET}"; exit 1; }

mkdir -p "$INSTALL_DIR" "$BIN_DIR"

if [ -d "$INSTALL_DIR/.git" ]; then
  say "${CYAN}updating existing install${RESET} in $INSTALL_DIR"
  git -C "$INSTALL_DIR" fetch --quiet origin
  git -C "$INSTALL_DIR" checkout --quiet "$BRANCH"
  git -C "$INSTALL_DIR" reset --quiet --hard "origin/$BRANCH"
else
  say "cloning into ${CYAN}$INSTALL_DIR${RESET}"
  git clone --quiet --branch "$BRANCH" --depth 1 "$REPO_URL" "$INSTALL_DIR"
fi

chmod +x "$INSTALL_DIR/cli/krypton" "$INSTALL_DIR/launcher/server.js" 2>/dev/null || true
ln -sf "$INSTALL_DIR/cli/krypton" "$BIN_DIR/krypton"

say "installed krypton CLI -> ${GREEN}$BIN_DIR/krypton${RESET}"
say "project at ${CYAN}$INSTALL_DIR${RESET}"
say ""
say "Next:"
say "  ${DIM}launch:${RESET}       $INSTALL_DIR/launcher/server.js  (then open http://localhost:5757)"
say "  ${DIM}or script:${RESET}    $INSTALL_DIR/run.sh"
say "  ${DIM}mods:${RESET}         krypton search sodium"
say "  ${DIM}build mod:${RESET}    (cd $INSTALL_DIR/mod && gradle build)"

case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *) say "${YELLOW:-}add to your shell: export PATH=\"$BIN_DIR:\$PATH\"${RESET}" ;;
esac
