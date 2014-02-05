
lazy val core = project in file("silk-core")

lazy val jena = project in file("silk-jena") dependsOn core

lazy val singlemachine = project in file("silk-singlemachine") dependsOn core dependsOn jena

lazy val learning = project in file("silk-learning") dependsOn core

lazy val workspace = project in file("silk-workspace") dependsOn core dependsOn jena dependsOn learning

lazy val workbench = project in file("silk-workbench") dependsOn workspace