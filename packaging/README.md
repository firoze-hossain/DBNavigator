# DBNavigator Packaging Suite

This directory contains resources and scripts for packaging DBNavigator into native cross-platform installers:

* **Linux**:
  * `linux/dbnavigator.desktop`: XDG desktop application shortcut
  * `linux/dbnavigator.png`: 512x512 application icon
  * `linux/postinst`: Post-install script for registering desktop shortcuts and icon caches
  * `linux/postrm`: Post-remove cleanup script
* **Windows**:
  * `windows/setup.iss`: Inno Setup compiler script for building `DBNavigator-Setup-<version>.exe`
  * `windows/DBNavigator.ico`: Multi-resolution Windows icon (16x16 to 256x256)
* **macOS**:
  * `macos/DBNavigator.icns`: High-resolution Apple ICNS icon for `.app` bundle and `.dmg` installer

Installers are built automatically via GitHub Actions upon pushing a version tag (e.g. `v1.0.0`).
