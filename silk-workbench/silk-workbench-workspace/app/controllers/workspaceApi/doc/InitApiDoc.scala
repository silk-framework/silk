package controllers.workspaceApi.doc

object InitApiDoc {

  final val initFrontendExample =
    """
      {
        "emptyWorkspace":true,
        "initialLanguage":"en",
        "dmBaseUrl": "http://docker.local",
        "maxFileUploadSize": 1000000000,
        "templatingEnabled": true,
        "version": "v21.11",
        "dmModuleLinks": [
                {
                    "defaultLabel": "Exploration",
                    "icon": "application-explore",
                    "path": "explore"
                },
                {
                    "defaultLabel": "Vocabulary Management",
                    "icon": "application-vocabularies",
                    "path": "vocab"
                },
                {
                    "defaultLabel": "Queries",
                    "icon": "application-queries",
                    "path": "query"
                }
            ]
      }
    """
}
