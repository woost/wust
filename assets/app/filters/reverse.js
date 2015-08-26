angular.module("wust.filters").filter("reverse", reverse);

reverse.$inject = [];

function reverse() {
  return items => items.slice().reverse();
}
