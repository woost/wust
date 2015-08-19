angular.module("wust.filters").filter("sorttags", sorttags);

sorttags.$inject = ["Helpers"];

function sorttags(Helpers) {
  return (items, ignoreTags) => {
      return Helpers.sortTags(_.reject(items, i => _.any(ignoreTags, _.pick(i, "id"))));
  };
}
