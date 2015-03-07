app.controller('VotesCtrl', function($scope){
    $scope.changes = [
    {
        action: 'delete',
        icon: 'fa-trash-o',
        affected: {
            node: {
                title: 'Du bist doof'
            }
        }
    },
    {
        action: 'create',
        icon: 'fa-star-o',
        affected: {
            node: {
                title: 'Ich bin zu dick'
            }
        }
    },
    {
        action: 'connect',
        icon: 'fa-compress',
        affected: {
            startnode: {
                title: 'Ich ernähre mich schlecht',
                class: 'discourse_problem'
            },
            relation: {
                title: 'causes'
            },
            endnode: {
                title: 'Ich bin zu dick',
                class: 'discourse_problem'
            }
        }
    },
    {
        action: 'disconnect',
        icon: 'fa-expand',
        affected: {
            startnode: {
                title: 'PC neustarten',
                class: 'discourse_idea'
            },
            relation: {
                title: 'solves'
            },
            endnode: {
                title: 'Ich ernähre mich schlecht',
                class: 'discourse_problem'
            }
        }
    },
    {
        action: 'flag',
        icon: 'fa-flag',
        affected: {
            node: {
                title: 'Buy Viagra!'
            },
        }
    },
    ];

});