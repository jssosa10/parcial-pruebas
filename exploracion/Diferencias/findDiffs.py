import re
import subprocess
f = open("diff.txt", "r")
commands = [x.replace("Files ","").replace(" differ\n","").split(" and ") for x in f]
for x in(commands):
	with open("diff-"+x[0].split("\\")[-1]+"-"+x[1].split("\\")[-1]+".txt", "w") as f:
		subprocess.run(["diff", x[0], x[1]], stdout=f)
