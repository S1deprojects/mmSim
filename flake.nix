{
  inputs = {
    flake-parts.url = "github:hercules-ci/flake-parts";
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    inputs@{ flake-parts, ... }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "aarch64-darwin"
        "aarch64-linux"
        "x86_64-darwin"
        "x86_64-linux"
      ];
      perSystem =
        { pkgs, self', ... }:
        {
          devShells.default = pkgs.mkShell {
            buildInputs = with pkgs; [
              graalvm-ce
              gradle
              jdt-language-server
            ];
            env = {
              JAVA_HOME = "${pkgs.graalvm-ce}";
              GRAALVM_HOME = "${pkgs.graalvm-ce}";
            };
          };
          packages = {
            default = self'.packages.mmsim-native;

            mmsim = pkgs.stdenvNoCC.mkDerivation (finalAttrs: {
              pname = "mmsim";
              version = "1.0-SNAPSHOT";

              src = builtins.path {
                path = ./.;
                name = "mmsim";
              };

              nativeBuildInputs = [
                pkgs.gradle
                pkgs.makeWrapper
              ];

              mitmCache = pkgs.gradle.fetchDeps {
                pkg = self'.packages.mmsim;
                data = ./deps.json;
              };

              # this is required for using mitm-cache on Darwin
              __darwinAllowLocalNetworking = true;

              installPhase = ''
                mkdir -p $out/{bin,share/mmsim}
                cp build/libs/mmsim-${finalAttrs.version}.jar $out/share/mmsim

                makeWrapper ${pkgs.jre}/bin/java $out/bin/mmsim \
                  --add-flags "-jar $out/share/mmsim/mmsim-${finalAttrs.version}.jar"
              '';

              meta.sourceProvenance = with pkgs.lib.sourceTypes; [
                fromSource
                binaryBytecode # mitm cache
              ];
            });

            mmsim-native =
              let
                jarPkg = self'.packages.mmsim;
              in
              pkgs.buildGraalvmNativeImage {
                pname = "mmsim";
                version = jarPkg.version;

                src = "${jarPkg}/share/mmsim/mmsim-${jarPkg.version}.jar";

                build-src = builtins.path {
                  path = ./.;
                  name = "mmsim";
                };
              };
          };
        };
    };
}
