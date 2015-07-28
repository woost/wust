angular.module("wust.elements")
    .directive("focusMe", ["$timeout", "$parse",
        function($timeout, $parse) {
            return {
                link: function(scope, element, attrs) {
                    var model = $parse(attrs.focusMe);
                    scope.$watch(model, function(value) {
                        if (value === true) {
                            $timeout(function() {
                                element[0].focus();
                            });
                        }
                    });
                    element.bind("blur", function() {
                        scope.$apply(model.assign(scope, false));
                    });
                }
            };
        }
    ]).directive("tagEditor", function() {
        // this directive relies on the node being a session from the editservice
        return {
            restrict: "AE",
            scope: {
                node: "=",
                getSuggestions: "&"
            },
            templateUrl: "assets/app/elements/ngTagEditor/ngTagEditor.html",
            controller: ["$scope", "$attrs", "$element", "$filter", "Tag",
                function($scope, $attrs, $element, $filter, Tag) {
                    $scope.suggestions = [];
                    $scope.search = "";

                    $scope.$watch("search", function(value) {
                        $scope.suggestions = $scope.getSuggestions({search: value});
                    });
                    $scope.add = function(tag) {
                        tag = tag.encode ? tag.encode() : tag;
                        $scope.node.addTag(tag);
                        $scope.search = "";
                    };
                    $scope.remove = function(index) {
                        // $scope.tags.splice(index, 1);
                    };

                    $element.find("input").on("keydown", function(e) {
                        if (e.which === 8) { /* backspace */
                            // if ($scope.search.length === 0 &&
                            //     $scope.tags.length) {
                            //     $scope.tags.pop();
                            //     e.preventDefault();
                            // }
                        } else if (e.which === 32 || e.which === 13) { /* space & enter */
                            let newTag = {title: $scope.search};
                            if (_.any($scope.node.tags, newTag)) {
                                $scope.search = "";
                            } else {
                                //TODO: we should not create a tag here.
                                Tag.$create(newTag).$then(tag => {
                                    $scope.add(tag);
                                });
                            }
                            e.preventDefault();
                        }
                    });
                }
            ]
        };
    });

