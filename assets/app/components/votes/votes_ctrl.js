angular.module("wust.components").controller("VotesCtrl", VotesCtrl);

VotesCtrl.$inject = [];

function VotesCtrl() {
    let vm = this;

    let changes = [{
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

    let changeindex = 0;
    vm.change = changes[changeindex];
    vm.showundo = false;

    vm.skip = skip;
    vm.undo = undo;

    function skip() {
        changeindex = (changeindex + 1) % changes.length;
        vm.change = changes[changeindex];
        vm.showundo = true;
    }

    function undo() {
        changeindex = (changes.length + changeindex - 1) % changes.length;
        vm.change = changes[changeindex];
    }

}
