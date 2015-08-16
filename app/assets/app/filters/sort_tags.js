angular.module("wust.filters").filter("sorttags", sorttags);

sorttags.$inject = ["Helpers"];

function sorttags(Helpers) {
  return items => Helpers.sortTags(items);
}
