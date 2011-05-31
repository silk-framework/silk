$(document).ready(function() {
	var $dialog = $('<div id="about_dialog"></div>')
		.html('<p>Silk Workbench, version 2.4</p>' +
    '<p>© 2009-2011</p>' +
    '<p style="font-weight: bold;">Website</p>' +
    '<p style="margin-top:5px;"><a href="http://www4.wiwiss.fu-berlin.de/bizer/silk/">http://www4.wiwiss.fu-berlin.de/bizer/silk/</a></p>' +
    '<p style="font-weight: bold;">Acknowledgements</p>' +
    '<p style="margin-top:5px;">This work was supported in part by Vulcan Inc. as part of its <a href="http://www.projecthalo.com">Project Halo</a> and by the EU FP7 project <a href="http://lod2.eu/">LOD2 - Creating Knowledge out of Interlinked Data</a> (Grant No. 257943).</p>')
		.dialog({
			autoOpen: false,
			title: 'About',
      width: 700,
      modal: true
		});
	$('#about').click(function() {
		$dialog.dialog('open');
		return false;
	});
});
