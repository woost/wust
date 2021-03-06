angular.module("wust.elements").directive("tagEditor", function() {
        return {
            restrict: "AE",
            replace: true,
            scope: {
                tags: "=",
                setFocus: "=",
                getSuggestions: "&",
                onChange: "&",
                existingOnly: "@",
                alwaysShow: "@",
                emptyShow: "@",
                editClassification: "@",
                placeholder: "@",
                embedSuggestions: "@",
            },
            templateUrl: "elements/ngTagEditor/ngTagEditor.html",
            controller: ["$scope", "$attrs", "$element", "$filter",
                function($scope, $attrs, $element, $filter) {
                    $scope.focus = false;
                    $scope.suggestions = [];
                    $scope.search = $scope.search || "";

                    let completeTabbing, ignoreNextSuggestion;
                    $scope.getSuggestions = $scope.getSuggestions ? $scope.getSuggestions : function() { return []; };

                    $scope.$watch("search", search);
                    $scope.$on("tageditor.suggestions", search);

                    function search() {
                        if (!ignoreNextSuggestion && completeTabbing === undefined) {
                            $scope.getSuggestions({search: $scope.search}).then(vals => {
                                $scope.suggestions = vals;
                            });
                        }

                        ignoreNextSuggestion = false;
                    }

                    $scope.unfocusedInput = function() {
                        if ($scope.search !== "") {
                            $scope.add({title: $scope.search});
                            $scope.search = "";
                        }
                    };

                    $scope.add = function(tag) {
                        if (_.trim(tag.title).length === 0)
                            return;

                        let allSuggestions = _.flatten($scope.suggestions);
                        completeTabbing = undefined;
                        let tagTitleLC = tag.title.toLowerCase();
                        tag = _.find(allSuggestions, t => t.title.toLowerCase() === tagTitleLC) || tag;
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
                            let allSuggestions = _.flatten($scope.suggestions);
                            if ($scope.search) {
                                if (completeTabbing === undefined) {
                                    if (allSuggestions.length > 0) {
                                        completeTabbing = $scope.search;
                                        $scope.search = allSuggestions[0].title;
                                        $scope.$apply();
                                        e.preventDefault();
                                    }
                                } else {
                                    let idx = _.findIndex(allSuggestions, {title: $scope.search});
                                    if (idx >= 0) {
                                        if (idx < allSuggestions.length - 1) {
                                            $scope.search = allSuggestions[idx + 1].title;
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

