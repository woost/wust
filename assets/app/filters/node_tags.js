angular.module("wust.filters")
    .filter("nodetags", nodetags)
    .filter("nodecontexts", nodecontexts)
    .filter("nodeclassifications", nodeclassifications);

nodetags.$inject = ["Helpers"];

function nodetags(Helpers) {
    return Helpers.sortedNodeTags;
}

nodecontexts.$inject = ["Helpers"];

function nodecontexts(Helpers) {
    return Helpers.sortedNodeContexts;
}

nodeclassifications.$inject = ["Helpers"];

function nodeclassifications(Helpers) {
    return Helpers.sortedNodeClassifications;
}
