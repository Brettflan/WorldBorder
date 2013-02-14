The things I changed (I hope I didn't forget anything)

plugin.yml:
	changed the help (<radius> --> <radiusX> <radiusZ>)
BorderCheckTask.java:
	nothing
BorderData.java:
	added variable radiusZ and renamed radius to radiusX
	changed the "handy data for borderChecks"
	adapted constructors to accept to radius-values
	added set-/getRadiusZ
	adapted toString function (it outputs "radiusX-radiusZ", I'm sure there a clearer way)
	changed the inside-border-check procedure for the ellipse (I added a comment, explaining why I think you could remove one check)
	changed the procedure that calculates the point inside the border the player need to be teleported to
	adapted the equals function
	adapted the hashCode function (not sure about this one, maybe you could improve it)
Config.java
	adapted the setBorder function to accept both radius-values
	adapted config-messages (round --> elliptic...)
	adapted the config-file-to-border function
	adapted the save procedure
CoordXZ.java:
	nothing
DynMapFeatures.java:
	adapted the showSquareBorder/showRoundBorder
WBCommand.java:
	changed all the commands to accept one parameter more (you always have to enter two values)
	changed the names of the shapes to rectangular and elliptic
	adapted the help (<radius> --> <radiusX> <radiusZ>)
	adapted the error messages to speak of two radius-values
WBListener.java:
	nothing
WorldBorder.java:
	nothing
WorldFileData.java:
	nothing
WorldFillTask.java:
	adapted calculation of the toFillBorder
	changed calculation of reportTarget
	added proposal for different method to calculate reportTarget
WorldTrimTask.java:
	adapted calculation of the toTrimBorder