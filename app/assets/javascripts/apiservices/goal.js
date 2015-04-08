angular.module("wust").factory("Goal", function(restmod) {
    return restmod.model("/goals").mix({
        goals: { hasMany: restmod.model("/goals") },
        problems: { hasMany: "Problem" },
        ideas: { hasMany: "Idea" },
    });
});
