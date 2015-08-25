#!/usr/bin/env node

var fs = require("fs");
var sane = require("sane");
var broccoli = require("broccoli");
var Watcher = require("broccoli-sane-watcher");
var ncp = require("ncp");
var path = require("path");
var mkdirp = require("mkdirp");

var arguments = process.argv.slice(2);

var appRoot = __dirname;
var destDir = arguments[0] || "public"
var brocfile = path.join(appRoot, "Brocfile.js");

console.log("Reading Brocfile", brocfile);
console.log("Writing to", destDir, "\n");

process.chdir(appRoot);

mkdirp(destDir);

var tree = require(brocfile);
var builder = new broccoli.Builder(tree);

var broccoliWatcher = new Watcher(builder, {verbose: true});
broccoliWatcher.on("change", function(hash) {
  console.log("Build finished in " + Math.round(hash.totalTime / 1e6) + " ms");
  ncp(hash.directory, path.join(appRoot, destDir));
  return hash;
});

broccoliWatcher.on("error", function(error) {
  console.log("ERROR", error.message);
});

process.addListener("exit", function () {
  console.log("exiting");
  builder.cleanup();
});

process.on("SIGINT", function () {
  process.exit(1);
});

process.on("SIGTERM", function () {
  process.exit(1);
});
