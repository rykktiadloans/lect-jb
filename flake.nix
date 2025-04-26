{
  description = "Lect-jb flake";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-unstable";
  };

  outputs = { self, nixpkgs }:
  let
    system = "x86_64-linux";
    pkgs = nixpkgs.legacyPackages.${system};
    usedPkgs = with pkgs; [
      xorg.libXext
      xorg.libX11
      xorg.libXrender
      xorg.libXrandr
      xorg.libXi
      xorg.libXxf86vm
      xorg.libXcursor
      freetype
      fontconfig
      libz
      e2fsprogs
    ];
  in
  {
    devShells.${system}.default =
      pkgs.mkShell
        {
          buildInputs = usedPkgs;

          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath(usedPkgs);

          shellHook = ''
            echo "Lect-jb flake started"
          '';

        };

    #packages.x86_64-linux.hello = nixpkgs.legacyPackages.x86_64-linux.hello;

    #packages.x86_64-linux.default = self.packages.x86_64-linux.hello;

  };
}
