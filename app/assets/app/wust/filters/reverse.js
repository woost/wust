angular.module("wust").filter("reverse", reverse);

reverse.$inject = [];

function reverse() {
  return items => items.slice().reverse();
}
