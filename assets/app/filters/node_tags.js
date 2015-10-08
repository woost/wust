angular.module("wust.filters").filter("nodetags", nodetags);

nodetags.$inject = ["Helpers"];

function nodetags(Helpers) {
  return (node, ignoreTags = []) => {
      return _.reject(Helpers.sortedNodeTags(node), i => _.any(ignoreTags, _.pick(i, "id")));
  };
}
