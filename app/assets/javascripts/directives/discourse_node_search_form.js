app.directive("discourseNodeSearchForm", function(DiscourseNode) {
    return {
        restrict: 'A',
        require: 'ngModel',
        templateUrl: 'assets/partials/discourse_node_search_form.html',
        scope: {
            ngModel: '=',
            onSubmit: '&',
            onSelect: '&',
            searchNodes: '&'
        },
        link: function($scope, element, attrs) {
            $scope.getNodes = getNodes;
            $scope.iconClass = attrs.iconClass;

            function getNodes(term) {
                return $scope.searchNodes()(term).$promise.then(function(response) {
                    return response.map(function(item) {
                        item.css = DiscourseNode.getCss(item.label);
                        return item;
                    });
                });
            }
        }
    };
});
