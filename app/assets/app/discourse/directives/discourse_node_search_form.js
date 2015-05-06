angular.module("wust.discourse").directive("discourseNodeSearchForm", discourseNodeSearchForm);

discourseNodeSearchForm.$inject = ["DiscourseNode"];

function discourseNodeSearchForm(DiscourseNode) {
    return {
        restrict: "A",
        require: "ngModel",
        templateUrl: "assets/app/discourse/directives/discourse_node_search_form.html",
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
            $scope.lastSearchResult = [];

            function getNodes(term) {
                return $scope.searchNodes({term: term}).$then(response => {
                    $scope.lastSearchResult = _.map(response, item => _.merge(item, {
                        css: DiscourseNode.get(item.label).css
                    }));
                }).$asPromise();
            }
        }
    };
}
