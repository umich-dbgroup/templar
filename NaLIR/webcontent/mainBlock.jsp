<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="architecture.*" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
  <title>NaliR</title>

<script src="http://code.jquery.com/jquery-latest.js"></script>
<script>
$(document).ready(function()
{
	$.post("conductCommand.jsp",
	{
		command: "abc" 
	},
	function(data,status)
	{
		feedback(data); 
	});

	$("#submitQuery").click(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "#query " + $("#inputSentence").val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
	$("#database").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "useDatabase " + $("#database").val() 
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
}); 

function feedback(data)
{
	var feedbacks = data.split("\n"); 
	var i = 0; 
	while(i < feedbacks.length)
	{
		var curFeedback = feedbacks[i].split(" "); 
		if(curFeedback[0] == "#history")
		{
			var history = "<datalist id=\"browsers\">"; 
			while(i < feedbacks.length && curFeedback[0] == "#history")
			{
				history += "<option value='" + feedbacks[i].substring(9) + "'>"; 
				i++; 
			}
			history += "</datalist>"; 

			document.getElementById("inputBlock").innerHTML = history;	
		}
		else if(curFeedback[0] == "#inputWord")
		{
//			document.getElementById("noMap").innerHTML = "";
			document.getElementById("useless").innerHTML = "";
			document.getElementById("map").innerHTML = "";
//			document.getElementById("generalIntepretation").innerHTML = "";	
//			document.getElementById("specificIntepretation").innerHTML = "";	
//			document.getElementById("error").innerHTML = "";	
//			document.getElementById("warning").innerHTML = "";	

			var sentence = ""; 
			while(i < feedbacks.length && curFeedback[0] == "#inputWord")
			{
				var word = curFeedback[2]; 
				for(var j = 3; j < curFeedback.length; j++)
				{
					word += " " + curFeedback[j]; 
				}
				sentence += "<U>" + word + "</U><sup><small>" + curFeedback[1] + "</small></sup> "; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			
			document.getElementById("inputWord").innerHTML = "<b>Your input is: </b>" + sentence;
		}
		else if(curFeedback[0] == "#deleted")
		{
			var result = ""; 
			while(i < feedbacks.length && curFeedback[0] == "#deleted")
			{
				var word = curFeedback[2]; 
				for(var j = 3; j < curFeedback.length; j++)
				{
					word += " " + curFeedback[j]; 
				}
				result += "<U>" + word + "</U><sup><small>" + curFeedback[1] + "</small></sup> "; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}

			if(result.length > 0)
			{
				document.getElementById("useless").innerHTML = "These words are <b>not</b> considered directly useful: " + result;
			}
		}
		else if(curFeedback[0] == "#map")
		{
			var results = "";
			while(i < feedbacks.length && curFeedback[0] == "#map")
			{
				var result = ""; 
				choices = feedbacks[i].split("; "); 					
				
				if(choices.length > 5 || choices[parseInt(choices[3])+4].split("#").length > 3)
				{
					result += choices[1] + "<sup><small>" + choices[2] + "</small></sup>" + " <b>maps to</b> " 
						+ "<select class = 'map' id = \"" + choices[2] + "\">"; 
				
					var selected = parseInt(choices[3])+4; 
					
					var stringName = ""; 
					for(var j = 4; j < choices.length; j++)
					{
						if(j == selected)
						{
							stringName = choices[j].split("#")[0]; 
							result += "<option value = \"" + (j-4).toString() + "\" selected = \"selected\">" + stringName + "</option>"; 
						}
						else
						{
							result += "<option value = \"" + (j-4).toString() + "\">" + choices[j] + "</option>"; 
						}
					}
					result += "</select>"; 

					if(choices[selected].split("#").length > 1)
					{
						var specifies = choices[selected].split("#"); 
						if(specifies.length > 3)
						{
							result += " <b>specifically</b> " + "<select class = 'specify' id = \"" + choices[2] + "\">"; 

							var selected = parseInt(specifies[1]); 
							if(specifies.length > 3)
							{
								if(selected == -1)
								{
									result += "<option value = \"" + selected + "\" selected = \"selected\">" + "any " + stringName + " containing \"" + choices[1] + "\"</option>"; 
								}
								else
								{
									result += "<option value = \"" + selected + "\">" + "any " + stringName + " containing " + choices[1] + "</option>"; 
								}
							}
							for(var j = 2; j < specifies.length; j++)
							{
								if(j == selected+2)
								{
									result += "<option value = \"" + (j-2).toString() + "\" selected = \"selected\">" + specifies[j] + "</option>"; 
								}
								else
								{
									result += "<option value = \"" + (j-2).toString() + "\">" + specifies[j] + "</option>"; 
								}
							}
							result += "</select>"; 					
						}
					}
					result += "<br>"; 
					results += result; 
				}
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			if(results.length > 0)
			{
				document.getElementById("map").innerHTML = results;
			}
		}
/*
		else if(curFeedback[0] == "general")
		{
			var result = "";
			var selected = curFeedback[1]; 
			i++; 
			var num = 0; 
			curFeedback = feedbacks[i].split(" "); 

			result += "Possible <b>approximate interpretations</b>: <br>"; 
			result += "<select id = \"setGeneralIntepretation\">"; 
			while(i < feedbacks.length && curFeedback[0] == "general")
			{
				var intepret = ""; 
				for(var j = 1; j < curFeedback.length; j++)
				{
					intepret += curFeedback[j] + " "; 
				}
				if(num == selected)
				{
					result += "<option value = \"" + num + "\" selected = \"selected\">" + intepret + "</option>"; 
				}
				else
				{
					result += "<option value = \"" + num + "\">" + intepret + "</option>"; 
				}
				i++;
				num++; 
				curFeedback = feedbacks[i].split(" "); 
			}
			result += "</select>"; 

			if(result.length > 1)
			{
				document.getElementById("generalIntepretation").innerHTML = result;
			}
		}
		else if(curFeedback[0] == "specific")
		{
			var result = "";
			var selected = curFeedback[1]; 
			i++; 
			var num = 0; 
			curFeedback = feedbacks[i].split(" "); 

			result += "Possible <b>accurate interpretations</b>: <br>"; 
			result += "<select id = \"setSpecificIntepretation\">"; 
			while(i < feedbacks.length && curFeedback[0] == "specific")
			{
				var intepret = ""; 
				for(var j = 1; j < curFeedback.length; j++)
				{
					intepret += curFeedback[j] + " "; 
				}
				if(num == selected)
				{
					result += "<option value = \"" + num + "\" selected = \"selected\">" + intepret + "</option>"; 
				}
				else
				{
					result += "<option value = \"" + num + "\">" + intepret + "</option>"; 
				}
				i++;
				num++; 
				curFeedback = feedbacks[i].split(" "); 
			}
			result += "</select>"; 

			if(result.length > 1)
			{
				document.getElementById("specificIntepretation").innerHTML = result;
			}
		}
		else if(curFeedback[0] == "result")
		{
			var result = "<b>Results: </b><br>"	+ "<table border = '1' style=\"margin-left:0.5em\">"; 
			var rowNum = -1; 
			while(i < feedbacks.length && curFeedback[0] == "result")
			{
				rowNum++; 
				var line = feedbacks[i].substring(7); 
				var row = line.split("###"); 
				result += "<tr>"; 
				for(var j = 0; j < row.length; j++)
				{
					result += "<td align = 'center' style = \"font-size:20px\">" + row[j] + "</td>"; 
				}
				result += "</tr>"; 
				i++; 
				curFeedback = feedbacks[i].split(" "); 
			}
			result += "</table>"; 
			if(rowNum > 0)
			{
				document.getElementById("queryResults").innerHTML = result;
			}
			else
			{
				document.getElementById("queryResults").innerHTML = "<b>Result is Empty</b>";
			}
		}
		else if(curFeedback[0] == "feedback")
		{
			var results = ""; 
			while(i < feedbacks.length && curFeedback[0] == "feedback")
			{
				results += feedbacks[i]; 
				i++; 
				if(i < feedbacks.length)
				{
					curFeedback = feedbacks[i].split(" "); 
				}
			}
			if(results.length > 0)
			{
				document.getElementById("feedbacks").innerHTML = results;
			}
		}
*/
		else
		{
			i++; 
		}
	}
	
	$(".map").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "#mapSchema " + $(this).attr("id") + " " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});	
	$(".specify").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "#mapValue " + $(this).attr("id") + " " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
/*
	$("#setGeneralIntepretation").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "generalIntepretation " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});

	$("#setSpecificIntepretation").change(function()
	{
		$.post("conductCommand.jsp",
		{
			command: "specificIntepretation " + $(this).val()
		},
		function(data,status)
		{
			feedback(data); 
		});
	});
*/
}
</script>

<%
CommandInterface system = new CommandInterface();  
session.setAttribute("system", system);  
%>
</head>

<body>
<h2><b><ins>Na</ins>tural <ins>L</ins>anguage <ins>I</ins>nterface over <ins>R</ins>elational Databases</b></h2>

<p>Use 
<select style = "font-size:13px" id = "database">
<option value = "MAS" selected = "selected">Microsoft Academic Search</option> 
<option value = "DBLP">DBLP</option> 
<option value = "YahooMovie">Yahoo!Movie</option> 
</select>
database.</p>

<b>Choose an Existing Query or type in a New Query: </b><br>
<input id = "inputSentence" list = "browsers" size="120" name="browser" style = "font-size:13px">			
<div id = "inputBlock" style="float:left"></div>
<button type ="button" id = "submitQuery">Submit</button>

<p id = "inputWord"></p>
<p id = "useless"></p>
<p id = "map"></p>

</body>
</html>