angular.module("wust.elements").directive("tagEditor", function() {
        return {
            restrict: "AE",
            scope: {
                tags: "=",
                getSuggestions: "&",
                onChange: "&",
                existingOnly: "@",
                alwaysShow: "@",
                initialFocus: "@"
            },
            templateUrl: "assets/app/elements/ngTagEditor/ngTagEditor.html",
            controller: ["$scope", "$attrs", "$element", "$filter",
                function($scope, $attrs, $element, $filter) {
                    $scope.setFocus = !!$scope.initialFocus;
                    $scope.suggestions = [];
                    $scope.search = "";
                    $scope.onChange = $scope.onChange ? $scope.onChange : function() {};
                    $scope.getSuggestions = $scope.getSuggestions ? $scope.getSuggestions : function() { return []; };

                    $scope.$watch("search", function(value) {
                        $scope.suggestions = $scope.getSuggestions({search: value});
                    });
                    $scope.add = function(tag) {
                        if ($scope.existingOnly && tag.id === undefined)
                            return;

                        tag = tag.encode ? tag.encode() : tag;
                        if (!_.any($scope.tags, tag)) {
                            $scope.tags.push(tag);
                            $scope.onChange();
                        }

                        $scope.search = "";
                    };
                    $scope.remove = function(index) {
                        $scope.tags.splice(index, 1);
                        $scope.onChange();
                    };

                    $element.find("input").on("keydown", function(e) {
                        if (e.which === 8) { /* backspace */
                            if ($scope.search.length === 0 &&
                                $scope.tags.length) {
                                $scope.$apply(function() {
                                    $scope.remove($scope.tags.length -1);
                                });
                                e.preventDefault();
                            }
                        } else if (e.which === 32 || e.which === 13) { /* space & enter */
                            $scope.$apply(function() {
                                $scope.add({ title: $scope.search });
                            });
                            e.preventDefault();
                        }
                    });
                }
            ]
        };
    });

