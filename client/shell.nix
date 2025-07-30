{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    jdk17
    gradle
    mesa
    libGL
    libGLU
    xorg.libX11
    xorg.libXrandr
    xorg.libXinerama
    xorg.libXcursor
    xorg.libXi
    wayland
    wayland-protocols
    libxkbcommon
    tree
  ];
  
  shellHook = ''
    export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath [
      pkgs.mesa
      pkgs.libGL
      pkgs.libGLU
      pkgs.xorg.libX11
      pkgs.xorg.libXrandr
      pkgs.xorg.libXinerama
      pkgs.xorg.libXcursor
      pkgs.xorg.libXi
      pkgs.wayland
      pkgs.libxkbcommon
    ]}:$LD_LIBRARY_PATH
  '';
}