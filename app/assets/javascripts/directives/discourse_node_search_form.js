angular.module("wust").directive("discourseNodeSearchForm", function(DiscourseNode) {
    return {
        restrict: "A",
        require: "ngModel",
        templateUrl: "assets/partials/discourse_node_search_form.html",
        scope: {
            ngModel: "=",
            onSubmit: "&",
            onSelect: "&",
            searchNodes: "&"
        },
        link: function($scope, element, attrs) {
            $scope.getNodes = getNodes;
            $scope.iconClass = attrs.iconClass;
            $scope.formatLabel = _.constant("");

            function getNodes(term) {
                return $scope.searchNodes()(term).$then(response => {
                    return _.map(response, item => _.merge(item, { css: DiscourseNode.get(item.label).css }));
                });
            }
        }
    };
});
