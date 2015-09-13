angular.module("wust.filters").filter("ignoretags", ignoretags);

ignoretags.$inject = ["Helpers"];

function ignoretags(Helpers) {
  return (items, ignoreTags) => {
      return _.reject(items, i => _.any(ignoreTags, _.pick(i, "id")));
  };
}
