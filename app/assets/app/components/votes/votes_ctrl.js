angular.module("wust.components").controller("VotesCtrl", function($scope) {
    $scope.changes = [{
        action: "delete",
        icon: "fa-trash-o",
        affected: {
            node: {
                title: "Du bist doof",
                class: "discourse_goal node"
            }
        }
    }, {
        action: "create",
        icon: "fa-star-o",
        affected: {
            node: {
                title: "Ich bin zu dick",
                class: "discourse_problem node"
            }
        }
    }, {
        action: "connect",
        icon: "fa-compress",
        affected: {
            startnode: {
                title: "Ich ernähre mich schlecht",
                class: "discourse_problem node"
            },
            relation: {
                title: "causes"
            },
            endnode: {
                title: "Ich bin zu dick",
                class: "discourse_problem node"
            }
        }
    }, {
        action: "disconnect",
        icon: "fa-expand",
        affected: {
            startnode: {
                title: "PC neustarten",
                class: "discourse_idea node"
            },
            relation: {
                title: "solves"
            },
            endnode: {
                title: "Ich ernähre mich schlecht",
                class: "discourse_problem node"
            }
        }
    }, {
        action: "flag",
        icon: "fa-flag",
        affected: {
            node: {
                title: "Buy Viagra!",
                class: "discourse_goal node"
            },
            flag: "spam"
        }
    }, ];

    $scope.changeindex = 0;
    $scope.change = $scope.changes[$scope.changeindex];
    $scope.showundo = false;

    $scope.support = skip;
    $scope.oppose = skip;
    $scope.skip = skip;
    $scope.undo = undo;

    function skip() {
        $scope.changeindex = ($scope.changeindex + 1) % $scope.changes.length;
        $scope.change = $scope.changes[$scope.changeindex];
        $scope.showundo = true;
    }

    function undo() {
        $scope.changeindex = ($scope.changes.length + $scope.changeindex - 1) % $scope.changes.length;
        $scope.change = $scope.changes[$scope.changeindex];
    }

});
