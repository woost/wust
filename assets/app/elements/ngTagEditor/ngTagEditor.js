angular.module("wust.elements").directive("tagEditor", function() {
        return {
            restrict: "AE",
            scope: {
                tags: "=",
                setFocus: "=",
                getSuggestions: "&",
                onChange: "&",
                existingOnly: "@",
                alwaysShow: "@",
                editClassification: "@",
                placeholder: "@"
            },
            templateUrl: "elements/ngTagEditor/ngTagEditor.html",
            controller: ["$scope", "$attrs", "$element", "$filter",
                function($scope, $attrs, $element, $filter) {
                    $scope.focus = false;
                    $scope.suggestions = [];
                    $scope.search = $scope.search || "";

                    let completeTabbing, ignoreNextSuggestion;
                    $scope.getSuggestions = $scope.getSuggestions ? $scope.getSuggestions : function() { return []; };

                    $scope.$watch("search", function(value) {
                        if (!ignoreNextSuggestion && completeTabbing === undefined) {
                            $scope.getSuggestions({search: value}).then(val => $scope.suggestions = val);
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
                        let tagTitleLC = tag.title.toLowerCase();
                        tag = _.find($scope.suggestions, t => t.title.toLowerCase() === tagTitleLC) || tag;
                        tag = tag.encode ? tag.encode() : tag;
                        if ($scope.existingOnly && tag.id === undefined)
                            return;

                        if (!_.any($scope.tags, t => t.title.toLowerCase() === tagTitleLC)) {
                            $scope.tags.push(tag);
                            if ($scope.onChange)
                                $scope.onChange({type: "add", tag: tag});
                        }

                        $scope.search = "";
                    };
                    $scope.remove = function(index) {
                        let tag = $scope.tags.splice(index, 1)[0];
                        if (tag !== undefined)
                            if ($scope.onChange)
                                $scope.onChange({type: "remove", tag: tag});
                    };

                    $element.find("input").on("keydown", function(e) {
                        if (e.which === 9) { /* tab */
                            if ($scope.search) {
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

