angular.module("wust.elements").directive("tagEditor", function() {
        return {
            restrict: "AE",
            scope: {
                tags: "=",
                setFocus: "=",
                getSuggestions: "&",
                onChange: "&",
                existingOnly: "@",
                alwaysShow: "@"
            },
            templateUrl: "ngTagEditor/ngTagEditor.html",
            controller: ["$scope", "$attrs", "$element", "$filter",
                function($scope, $attrs, $element, $filter) {
                    $scope.focus = false;
                    $scope.suggestions = [];
                    $scope.search = $scope.search || "";
                    $scope.onChange = $scope.onChange ? $scope.onChange : function() {};

                    let completeTabbing, ignoreNextSuggestion;
                    $scope.getSuggestions = $scope.getSuggestions ? $scope.getSuggestions : function() { return []; };

                    $scope.$watch("search", function(value) {
                        if (!ignoreNextSuggestion && completeTabbing === undefined) {
                            $scope.getSuggestions({search: value}).$then(val => $scope.suggestions = val);
                        }

                        ignoreNextSuggestion = false;
                    });

                    $scope.unfocusedInput = function() {
                        if ($scope.search !== "") {
                            $scope.add({title: $scope.search});
                            $scope.search = "";
                        }
                    };

                    $scope.add = function(tag) {
                        if (_.trim(tag.title).length === 0)
                            return;

                        completeTabbing = undefined;
                        tag = _.find($scope.suggestions, _.pick(tag, "title")) || tag;
                        tag = tag.encode ? tag.encode() : tag;
                        if ($scope.existingOnly && tag.id === undefined)
                            return;

                        if (!_.any($scope.tags, _.pick(tag, "title"))) {
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
                        if (e.which === 9) { /* tab */
                            if (completeTabbing === undefined) {
                                if ($scope.suggestions.length > 0) {
                                    completeTabbing = $scope.search;
                                    $scope.search = $scope.suggestions[0].title;
                                    $scope.$apply();
                                    e.preventDefault();
                                }
                            } else {
                                let idx = _.findIndex($scope.suggestions, {title: $scope.search});
                                if (idx >= 0) {
                                    if (idx < $scope.suggestions.length - 1) {
                                        $scope.search = $scope.suggestions[idx + 1].title;
                                    } else {
                                        $scope.search = completeTabbing;
                                        ignoreNextSuggestion = true;
                                        completeTabbing = undefined;
                                    }
                                    $scope.$apply();
                                    e.preventDefault();
                                }
                            }
                        } else if (e.which === 8) { /* backspace */
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
                        } else {
                            completeTabbing = undefined;
                        }
                    });
                }
            ]
        };
    });

