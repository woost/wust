angular.module("wust.config").config(PrototypeExtensions);

PrototypeExtensions.$inject = [];

function PrototypeExtensions() {
    Boolean.prototype.implies = function(other) { return !this.valueOf() || other; };
}
