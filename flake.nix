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
        { pkgs, ... }:
        {
          devShells.default = pkgs.mkShell {
            buildInputs = with pkgs; [
              gcc
              graalvm-ce
              gradle
              jdt-language-server
            ];
            env = {
              JAVA_HOME = "${pkgs.graalvm-ce}";
              GRAALVM_HOME = "${pkgs.graalvm-ce}";
            };
          };
        };
    };
}
