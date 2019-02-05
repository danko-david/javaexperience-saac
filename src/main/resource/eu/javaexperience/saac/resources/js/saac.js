
/**************************** Common functions ********************************/

window.SAAC_LOG = LoggingFacility.getByName("SAAC");
/*window.SAAC_LOG = new function()
{
	this.may = function(){return false;}
	this.trace =
	this.debug =  function(){};
};*/


//
// Template related functions
//

//TODO assert mustache is available

function renderTemplate(/*String*/name, /*Object*/ ctx)
{
	var template = $('[data-template="'+name+'"]').html();
	if(undefined == template)
	{
		return "template doesn't exists: "+name;
	}
	//Mustache.parse(template);
	//return Mustache.render(template, ctx);
	return template;
}

/**
 * type: info, warning, errorsaac_initEntryPoint
 * */
function notify_user(type, text)
{
	toastr[type](text);
}

function copy_to_clipboard(txt)
{
	var elem = parseHtml('<textarea></textarea>');
	elem.innerText = txt;
	document.body.appendChild(elem);
	elem.select();
	document.execCommand('copy');
	elem.remove();
}

/********************** Typing system related functions ***********************/

function shorten_type(str)
{
	str = str.getSubstringAfterLastString(".");
	str = str.replaceAll(";", "[]");
	return str;
}

function saac_type_get_typeclass(t)
{
	if(is_valuable(t.genericComponentType))
	{
		return saac_type_get_typeclass(t.genericComponentType);
	}
	
	if(is_valuable(t["class"]))
	{
		return "primitive";
	};
	
	if(is_valuable(t.rawType))
	{
		var ret = "";
		var cls = shorten_type(t.rawType["class"]);
		
		var subj_func =
				cls.startsWith("GetBy")
			||
				"SimpleGet" == cls;
		
		var func_no_ret = cls.startsWith("SimplePublish") || "SimpleCall" == cls;
		
		if(subj_func  || func_no_ret)
		{
			return "function";
		}
	}
	else
	{
		return "compaund";
	}
	
	return "unknown";
}

function saac_type_render(t)
{
	if(is_valuable(t.rawType))
	{
		var ret = "";
		var cls = shorten_type(t.rawType["class"]);
		
		var subj_func =
				cls.startsWith("GetBy")
			||
				"SimpleGet" == cls;
		
		var func_no_ret = cls.startsWith("SimplePublish") || "SimpleCall" == cls;
		
		subj_func |= func_no_ret;
		
		var off = 0;
		
		if(subj_func)
		{
			if(func_no_ret)
			{
				ret += "void <- (";
				off = 0;
			}
			else
			{
				ret += saac_type_render(t.actualTypeArguments[0]);
				ret += " <- (";
				off = 1;
			}
		}
		else
		{
			ret += cls;
			ret += "<";
		}
		
		for(var i=off;i<t.actualTypeArguments.length;++i)
		{
			if(i != off)
			{
				ret += ", ";
			}
			ret += saac_type_render(t.actualTypeArguments[i]);
		}
		
		ret += subj_func?")":">";
		return ret;
	}
	
	if(is_valuable(t.genericComponentType))
	{
		return saac_type_render(t.genericComponentType)+"[]";
	}
	
	if(is_valuable(t["class"]))
	{
		return shorten_type(t["class"]);
	};
	
	if(is_valuable(t.genericDeclaration))
	{
		return "<"+t.name+">";
	}
	
	if(SAAC_LOG.may(LOGLEVEL.error))
	{
		SAAC_LOG.error(["unknown type ??: ", arguments, new Error().stack]);
	}
	return "??";
	//return renderTemplate("saac/type", obj);
}

function saac_type_is_variadic(type)
{
	if(is_valuable(type.genericComponentType))
	{
		return true;
	}
	
	if(saac_type_render(type).endsWith("[]"))
	{
		return true;
	}
	/*if(SAAC_LOG.may(LOGLEVEL.trace))
	{
		SAAC_LOG.trace(["is_variadic", saac_type_render(type), ret, type]);
	}*/
	return false;
}

function saac_is_function_descriptor(item)
{
	return is_valuable(item)
			&& is_valuable(item.id)
			&& is_valuable(item.name)
			&& is_valuable(item.returning)
			&& is_valuable(item.arguments) 
}

//TODO
function saac_is_type_acceptable(type_to_assign, type)
{
	


}

//TODO RPC: getFunctionProperties
function saac_validate_arguments(/*Html*/ container)
{
	//using saac_is_type_acceptable

}

function saac_assert_valid_restore_data(/*object*/ data)
{
	
}

function saac_restore_function(/*Html*/ container, /*Object*/ data)
{
	if(SAAC_LOG.may(LOGLEVEL.debug))
	{
		SAAC_LOG.debug(["Restoring function into container: ", container, data]);
	}
	//const value
	var f_ac = container.querySelector(".saac_function_autocomplete");
	if("ta" == data.it)
	{
		f_ac = f_ac.alterTag("textarea");
	}
	
	f_ac.setContent(data.search);
	
	if("ta" == data.it)
	{
		update_textarea_size(container.querySelector(".saac_function_autocomplete"));
	}
	
	saac_update_function_ac_input(f_ac);
	
	if(is_valuable(data.pa) && data.pa)
	{
		container.querySelector(".saac_view_arguments_mode").click();
	}
	
	if(is_valuable(data.id) && "" != data.id)
	{
		var func = saac_get_function_by_id(data.id);
		if(!is_valuable(func))
		{
			throw new Exception("Invalid function id: "+data.id);
		}
		
		saac_function_container_set_function(container, func);

		//restore args/varargs
		if(is_valuable(data.args))
		{
			saac_restore_function_arguments(container, data.args);
		}
	}
}

function saac_restore_function_arguments(/*Html*/ container, /*Object[]*/ args)
{
	saac_ensure_arguments_container_function_container_count
	(
		container,
		args.length
	);
	
	//ac is an array of .saac_argument that may be .saac_arg_varadric
	var ac = container.querySelector(".saac_f_arguments").children;
	
	if(SAAC_LOG.may(LOGLEVEL.trace))
	{
		SAAC_LOG.trace(["Restoring arguments", ac, args]);
	}
	
	for(var i=0;i<ac.length;++i)
	{
		//i-th html elem
		var subj = ac[i];
		var vs = args[i];
		
		if(SAAC_LOG.may(LOGLEVEL.trace))
		{
			SAAC_LOG.trace(["Restoring argument parameter", i, subj, vs]);
		}
		
		if(is_valuable(vs.length)/*subj.is(".saac_arg_varadric")*/)
		{
			for(var n = 1;n<vs.length;++n)
			{
				saac_add_varadic(subj);
			}
			
			//getting the i-th varadic parameter's containier (it's a function container)
			var target = subj.querySelector(".saac_arg_container").children;
			
			for(var n = 0;n<vs.length;++n)
			{
				if(SAAC_LOG.may(LOGLEVEL.trace))
				{
					SAAC_LOG.trace
					(
						[
							"restore varadic to container:",
							target[n],
							vs[n],
							"in:",
							subj
						]
					);
				}
				
				saac_restore_function
				(
					target[n],
					vs[n]
				);
			}
		}
		else
		{
			saac_restore_function
			(
				saac_find_container_dir_child(subj),
				vs
			);
		}
	}
}


/*********************** Dom navigation/modifier utils ************************/

function saac_get_top_container(container)
{
	return container.whereParentWithBoundary(".saac_entry_point", ".saac_entry_point");
}


function saac_is_container_in_use(/*Html*/ container)
{
	return "" !== container.querySelector(".saac_f_function_id").value;
}


function saac_set_function_container_type(/*Html*/ container, /*?Object*/ type)
{
	var t = saac_type_get_typeclass(type);
	container.querySelector(".f_wrapper").value = t;
	container.querySelector(".saac_function_autocomplete").placeholder = t;
}

function saac_function_container_set_function(container, func)
{
	//saac_set_function_container_type(container, func.returning);
	var func_id = "";
	var wrapper_type = "";
	var ret_type = "";
	var func_descr = "";
	
	if(is_valuable(func))
	{
		func_id = safe_get_attr(func, "id") || null;
		ret_type = saac_type_render(func.returning.type);
		func_descr = func.descr;
	}
	else
	{
		container.querySelector(".saac_function_autocomplete").value = "";
	}
	
	container.querySelector(".saac_f_function_id").value = func_id;
	container.querySelector(".f_wrapper").value = wrapper_type;
	
	container.querySelector(".saac_f_ret_type").innerText = ret_type;
	container.querySelector(".saac_function_description").innerText = func_descr;
	
	saac_ensure_argument_containers(container, null == func? null: func.arguments);
}

function saac_update_arg_infos
(
	/*Html.saac_argument*/ arg_container,
	/*Object*/ arg_info
)
{
	//primitive
	//function
	//TODO: fwrapper
	
	var pcont = arg_container.querySelector(".saac_function_container");
	if(!saac_is_container_in_use(pcont))
	{
		saac_set_function_container_type(pcont, arg_info.type);
	}
	
	if(saac_type_is_variadic(arg_info.type))
	{
		arg_container.classList.add("saac_arg_varadric");
	}
	else
	{
		arg_container.classList.remove("saac_arg_varadric");
	}
	
	arg_container.querySelector(".saac_arg_type").innerText = saac_type_render(arg_info.type);
	arg_container.querySelector(".saac_arg_name").innerText = arg_info.name;
	arg_container.querySelector(".saac_arg_descr").innerText = arg_info.description;
}

function saac_ensure_arguments_container_function_container_count
(
	/*Html.saac_argument*/ container,
	/*int*/ creq
)
{
	var tar_args = container.querySelector(".saac_f_arguments");
	var cnums = tar_args.childElementCount;
	
	//add new one argument containers if needed
	if(creq > cnums)
	{
		for(;creq > cnums;cnums++)
		{
			var html = parseHtml(renderTemplate("saac/argument"));
			var func = parseHtml(renderTemplate("saac/function_container"));
			html.querySelector(".saac_arg_container").appendChild(func);
			tar_args.appendChild(html);
		}
	}
	
	//delete plus containers (as much we can)
	if(creq < cnums)
	{
		for(var i=cnums-1;i >= creq;--i)
		{
			if(!saac_is_container_in_use(tar_args.children[i].querySelector(".saac_arg_container")))
			{
				tar_args.children[i].remove();
			}
		}
	}
}

/**
 * Ensures the argument container exists:
 * 	- creates the new ones for the requised argument, but keep the sub conatiner.
 * 	- deletes the extra argument containers at the end (if empty)
 * 	- creates/remotes add/delete button(as a container) if type is variadic (or array).
 * 	=> validates conatiner
 * */
function saac_ensure_argument_containers
(
	/*Html.saac_argument*/ container,
	/*?array[args]*/ args
)
{
	var tar_args = container.querySelector(".saac_f_arguments");
	var creq = null == args?0:args.length;
	
	saac_ensure_arguments_container_function_container_count
	(
		container,
		creq
	);
	
	//update paramater infos (type name descr)
	
	var tarc = tar_args.children;
	for(var i=0;i<creq;++i)
	{
		saac_update_arg_infos(tarc[i], args[i]);
	}
	
	saac_validate_arguments(container);
}

function saac_find_container_dir_parent(/*Html*/ elem)
{
	return elem.whereParentWithBoundary(".saac_function_container", ".saac_entry_point");
}

function saac_find_container_dir_child(/*Html*/ elem)
{
	return elem.querySelector(".saac_function_container");
}

function saac_find_container_dir_child_all(/*Html.saac_f_arguments*/ elem)
{
	var chk = elem.querySelector(".saac_function_container");
	if(is_valuable(chk))
	{
		var cs = chk.parentNode.children;
		/*if(1 == cs.length)
		{
			return chk;
		}
		else if(1 < cs.length)
		{*/
			return cs;
		//}
	}
	return null;
}

/**
 * Selects the nearest descendant .saac_f_arguments and returns
 * 	with the .saac_argument elements
 * */
function saac_find_container_arguments(/*Html*/ container)/*: array[Html]*/
{
	var args = container.querySelector(".saac_f_arguments");
	var ret = [];
	var c = args.children;
	for(var i=0;i< c.length;++i)
	{
		if(c[i].is(".saac_argument"))
		{
			ret.push(c[i]);
		}
	}
	return ret;
}

function saac_add_varadic(/*Html.saac_argument*/ container)
{
	var ac =
		//container;
		container.querySelector(".saac_arg_container");
	
	var func = parseHtml(renderTemplate("saac/function_container"));
	ac.appendChild(func);	
}

function wipe_conainer(/*Html*/ container)
{
	//remove function params
	var c = saac_find_container_arguments(container);
	for(var i=0;i<c.length;++i)
	{
		c[i].remove();
	}
	saac_function_container_set_function(container, null);
}

function saac_get_function_by_id(/*String*/ id)/*: Object*/
{
	for(var i=0;i < window.SAAC_FUNCTIONS.length;++i)
	{
		if(id == window.SAAC_FUNCTIONS[i].id)
		{
			return window.SAAC_FUNCTIONS[i];
		}
	}
	return null;
}

function saac_get_function_by_name(/*String*/ name)/*: Object*/
{
	for(var i=0;i < window.SAAC_FUNCTIONS.length;++i)
	{
		if(name == window.SAAC_FUNCTIONS[i].name)
		{
			return window.SAAC_FUNCTIONS[i];
		}
	}
	return null;
}

function saac_determine_nth_arg_iam(container)
{
	var ac = container.whereParentWithBoundary(".saac_argument", ".saac_entry_point");
	if(!is_valuable(ac))
	{
		return null;
	}
	
	return ac.index();
}

function saac_container_get_function(container)
{
	var id = container.querySelector(".saac_f_function_id").value;
	return saac_get_function_by_id(id);
}


/********************** Communication/serialization utils *********************/

function __is_opt_set(/*int*/ subject, /*int*/ test)
{
	return subject & test == test;
}


SAAC_SERIALIZATION_SELECT_MODE =
{
	NONE:	0,
	DIRECT:	1,
	ALL:	2
}

function saac_serialize_container
(
	/*Html*/ container,
	/*int: SAAC_SERIALIZATION_SELECT_MODE*/ parent_mode,
	/*int: SAAC_SERIALIZATION_SELECT_MODE*/ child_mode
)
{
	if(!is_valuable(container))
	{
		return null;
	}
	
	var ret = {};
	
	ret.id = container.querySelector(".saac_f_function_id").value;
	ret.search = container.querySelector(".autocomplete").value;
	ret.it = "TEXTAREA" == container.querySelector(".autocomplete").nodeName?"ta":undefined;
	ret.pa = container.querySelector(".saac_view_arguments_mode").classList.contains("glyphicon-resize-horizontal");
	
	O = SAAC_SERIALIZATION_SELECT_MODE;
	if(0 != parent_mode)
	{
		ret.parent = saac_serialize_container
		(
			saac_find_container_dir_parent(container),
			parent_mode == O.ALL?O.ALL:0,
			0
		);
	}
	
	if(0 != child_mode)
	{
		var c = [];
		var chps = saac_find_container_arguments(container);
		
		for(var n=0;n<chps.length;++n)
		{
			var argcont = chps[n];
			if(argcont.is(".saac_arg_varadric"))
			{
				var all = saac_find_container_dir_child_all(argcont);
				var add = [];
				if(is_valuable(all))
				{
					for(var i=0;i<all.length;++i)
					{
						add.push
						(
							saac_serialize_container
							(
								all[i],
								0,
								child_mode == O.ALL?O.ALL:0
							)
						);
					}
				}
				c.push(add);
			}
			else
			{
				c.push
				(
					saac_serialize_container
					(
						saac_find_container_dir_child(argcont),
						0,
						child_mode == O.ALL?O.ALL:0
					)
				);
			}
			
		}
		ret.args =  c;
	}
	
	return ret;
}

/**************************** Bookeeping functions ****************************/

//TODO locailze to the top container
window.SAAC_FUNCTIONS = null;

function __saac_refresh_function_list(RPC, onRefreshDone)
{
	if(is_valuable(window.SAAC_FUNCTIONS))
	{
		if(is_function(onRefreshDone))
		{
			onRefreshDone(window.SAAC_FUNCTIONS);
		}
	}
	else
	{
		RPC.listFunctions
		(
			function(ret)
			{
				window.SAAC_FUNCTIONS = ret;
				if(is_function(onRefreshDone))
				{
					onRefreshDone(ret);
				}
			}
		);
	}
}

/**
 * check param status:
 * 		- green		=> assembly time ensurance
 * 		- yellow	=> valua availabe in runtime (wrapped for runtime lookup)
 * 		- red		=> incompatible type
 * 
 * TODO
 */
function saac_update_container_validity(/*Html.saac_function_container*/ container)
{
	var cl = container.classList;
	cl.remove("saac_function_ok_assembly");
	cl.remove("saac_function_ok_runtime");
	cl.remove("saac_function_fail");
	
	
	
	
	

}

/************ The module's autocomplete related handler functions *************/

function __saac_handle_autcomplete(RPC, container, process)
{
	var cls = container.querySelector(".f_wrapper").value;
	
	if("" == cls || "function" == cls)
	{
		//todo internal recommendation from the downloaded list.
		//__saac_refresh_function_list(RPC, process);
		var parentFunctionContainer = saac_find_container_dir_parent(container);
		
		var parentFunction = null;
		var nth_arg = -1;
		var varadic = false;
		
		if(is_valuable(parentFunctionContainer))
		{
			parentFunction = saac_container_get_function(parentFunctionContainer);
			nth_arg = saac_determine_nth_arg_iam(container);
			varadic = container.whereParentWithBoundary(".saac_argument", ".saac_entry_point").classList.contains("saac_arg_varadric")
		}
		else
		{
			//getting the root type:
			var tr = container.whereParent(".saac_entry_point").querySelector(".saac_root_accept_type").innerText;
			if(is_valuable(tr) && "" != tr)
			{
				var arr = tr.split(":");
				if(is_valuable(arr) && arr.length > 1)
				{
					parentFunction = saac_get_function_by_id(arr[0]);
					nth_arg = parseInt(arr[1]);
				}
			}
		}
	
		SAAC_LOG.may(LOGLEVEL.debug)
		{
			SAAC_LOG.debug
			(
				[
					"Offering request via autocomplete:",
					container,
					parentFunction,
					nth_arg
				]
			);
		}
		
		var input = container.querySelector(".saac_function_autocomplete");
		
		if(!is_valuable(parentFunction) || nth_arg < 0)
		{
			process(SAAC_FUNCTIONS);
		}
		else
		{
			RPC.offerForType
			(
				function(ret)
				{
					if(null == ret)
					{
						process(SAAC_FUNCTIONS);
					}
					else
					{
						process(ret);
					}
				},
				is_valuable(parentFunction)?parentFunction.id:null,
				nth_arg,
				varadic,
				input.getContent(),
				input.selectionStart,
				input.selectionEnd
			);
		}
	}
	else
	{
		//TODO if type is an enum
	}
}

function saac_update_function_ac_input(input)
{
	//unset function container
	if("" == input.value)
	{
		saac_function_container_set_function(saac_find_container_dir_parent(input), null);
	}
	
	var min = input.dataset.originsize;
	var tar = input.value.length + 1;
	if(tar < min)
	{
		tar = min;
	}
	input.size = tar;
}

function __saac_bound_autocomplete(RPC, target)
{
	$(target).typeahead
	(
		{
			highlight: true,
			minLength: 0,
			limit: 15,
			maxItem: 15,
			displayText: function(item)
			{
				return item.name;
			},
			afterSelect: function(item)
			{
				if(is_valuable(this.currentlySelectedItem))
				{
					this.$element[0].value = this.currentlySelectedItem.name;
				}
			},
			source: function (query, process)
			{
				var container = saac_find_container_dir_parent(target);
				__saac_handle_autcomplete(RPC, container, process);
			},
			updater: function(item)
			{
				if(saac_is_function_descriptor(item))
				{
					saac_function_container_set_function(saac_find_container_dir_parent(target), item);
				}
				this.currentlySelectedItem = item;
				return false;
			}
		}
	)
	.keyup
	(
		function()
		{
			saac_update_function_ac_input(target);
		}
	);
}

function __saac_add_eventlistener_autocomplete(RPC)
{
	//binding autocomplete for new inputs
	document.body.addEventListener
	(
		"focus",
		function(event)
		{
			var wcls = "saac_typeahead_wired";
			
			target = event.target;
			
			//prevent handling unwanted events
			if(!target.classList.contains("saac_function_autocomplete"))
			{
				return;
			}
			
			if(!target.classList.contains(wcls))
			{
				__saac_bound_autocomplete(RPC, target);
				target.classList.add(wcls);
				$(target).trigger("focus");
			}
		},
		true
	);
}

function update_textarea_size(tar)
{
	fs = window.getComputedStyle(tar).fontSize;
	console.log("update size "+tar.scrollWidth+" "+tar.scrollHeight+" fs :"+fs);
	cnt = tar.getContent();
	var lines = cnt.split("\n");
	var max = 0; 
	for(var i= 0;i<lines.length;++i)
	{
		var len = (lines[i].split("\t").length-1)*3+lines[i].length;
		if(len > max)
		{
			max = len;
		}
	}
	
	max /= 2;
	
	tar.style.width = (parseInt(fs)*max)+"px";
	tar.style.height = (tar.scrollHeight)+"px";
}

function __saac_add_eventlistener_textarea(RPC)
{
	document.body.addEventListener
	(
		"keyup",
		function(e)
		{
			var tar = e.target;
			if("TEXTAREA" != tar.nodeName || !tar.classList.contains("saac"))
			{
				return;
			}
			
			update_textarea_size(tar);
		}
	);
}

function __saac_add_eventlistener_argument_sortable(RPC)
{
	//sortable parameters
}


/***************** The module's click related handler functions ***************/ 

function __do_copy_clipboard(target, notify)
{
	var main = target.whereParentWithBoundary(".saac_entry_point, .saac_argument", ".saac_entry_point");
	if(is_valuable(main))
	{
		var container = saac_find_container_dir_child(main);
		if(is_valuable(container))
		{
			var ser = saac_serialize_container
			(
				container,
				SAAC_SERIALIZATION_SELECT_MODE.ALL,
				SAAC_SERIALIZATION_SELECT_MODE.ALL
			);
			
			copy_to_clipboard(JSON.stringify(ser));
			
			if(notify)
			{
				notify_user("info", "Másolva a vágólapra");
			}
		}
	}
}

function __do_wipe_container(target, notify)
{
	var main = target.whereParentWithBoundary(".saac_entry_point, .saac_argument", ".saac_entry_point");
	if(is_valuable(main))
	{
		var container = saac_find_container_dir_child(main);
		if(is_valuable(container))
		{
			wipe_conainer(container);
			if(notify)
			{
				notify_user("info", "Eljárás eltávolítva");
			}
		}
	}
}

function __do_cut_clipboard(target)
{
	__do_copy_clipboard(target, false);
	__do_wipe_container(target, false);
	notify_user("info", "Eljárás áthelyezve a várólapra");
}


function __do_server_execute(RPC, target)
{
	var main = target.whereParentWithBoundary(".saac_entry_point", ".saac_entry_point");
	if(is_valuable(main))
	{
		var container = saac_find_container_dir_child(main);
		if(is_valuable(container))
		{
			RPC.execute
			(
				function(ret, error)
				{
					if(SAAC_LOG.may(LOGLEVEL.debug))
					{
						SAAC_LOG.debug(["EXEC: ", arguments]);
					}
					if(is_valuable(error))
					{
						notify_user("error", "Szerver oldali hiba történt: "+error);
					}
					else if(is_valuable(ret))
					{
						notify_user("info", "Szerver oldali futtatás megkezdve.");
					}
					else
					{
						notify_user("error", "Belső hiba történt, részletekért lásd a javascript konzolt.");
					}
				},
				saac_serialize_container
				(
					container,
					SAAC_SERIALIZATION_SELECT_MODE.ALL,
					SAAC_SERIALIZATION_SELECT_MODE.ALL
				)
			);
		}
	}
}

function __do_varadic_add(target)
{
	saac_add_varadic(target.whereParentWithBoundary(".saac_argument"), ".saac_entry_point");
}

function __do_varadic_remove(target)
{
	target.whereParentWithBoundary(".saac_function_container", ".saac_entry_point").remove();
}

function __do_arguments_viewmode_switch(target)
{
	target.changeClass("glyphicon-resize-vertical", "glyphicon-resize-horizontal");
	var container = saac_find_container_dir_parent(target);
	if(target.classList.contains("glyphicon-resize-horizontal"))
	{
		container.classList.add("saac_viewmode_horizontal");
	}
	else
	{
		container.classList.remove("saac_viewmode_horizontal");
	}
}

function __do_show_console(/*Html*/ target)
{
	var top = saac_get_top_container(target);
	if(is_valuable(top.consoleDom))
	{
		return;
	}
	
	var consoleContainer = top.querySelector(".saac_hidden_console_content");
	
	var win = window.open("", "B", "width=800,height=600");
	win.document.title = "Saac Console";
	win.document.body.appendChild(consoleContainer.children[0]);
	consoleContainer.innerHtml = "";
	
	win.onbeforeunload = function()
	{
		consoleContainer.appendChild(win.document.body.children[0]);
		
		top.console = undefined;
		top.consoleDom = undefined;
		target.classList.remove("saac_disabled");
	}
	
	top.console = win;
	top.consoleDom = win.document;
	target.classList.add("saac_disabled");
}

function __saac_add_eventlistener_clicks(RPC)
{
	document.body.addEventListener
	(
		"click",
		function(event)
		{
			var target = event.target;
			//prevent handling unwanted events
			if(!target.classList.contains("saac"))
			{
				return;
			}
			
			//dispatching menubar and ...TODO... events;
			if(target.classList.contains("saac_operation_all_clipboard"))
			{
				__do_copy_clipboard(target, true);
			}
			else if(target.classList.contains("saac_operation_remove"))
			{
				__do_wipe_container(target, true);
			}
			else if(target.classList.contains("saac_operation_clipboard_cut"))
			{
				__do_cut_clipboard(target);
			}
			else if(target.classList.contains("saac_operation_execute"))
			{
				__do_server_execute(RPC, target);
			}
			else if(target.classList.contains("saac_varadic_add"))
			{
				__do_varadic_add(target);
			}
			else if(target.classList.contains("saac_varadic_remove"))
			{
				__do_varadic_remove(target);
			}
			else if(target.classList.contains("saac_view_arguments_mode"))
			{
				__do_arguments_viewmode_switch(target);
			}
			else if(target.classList.contains("saac_operation_show_console"))
			{
				if(!target.classList.contains("saac_disabled"))
				{
					__do_show_console(target);
				}
			}
		}
	);
}

function __do_funtion_paste(/*Html*/ target, /*String*/ data)
{
	try
	{
		var container = null;
		if(target.classList.contains("saac_function_container"))
		{
			container = target;
		}
		else
		{
			container = saac_find_container_dir_parent(target);
		}
		
		var json = JSON.parse(data);
		
		//TODO validate
		saac_assert_valid_restore_data(json);
		__do_wipe_container(container, false);
		saac_restore_function(container, json);
		notify_user("info", "Sikeres beillesztés");
	}
	catch(err)
	{
		notify_user("error", err.message);
		throw err;
	}
}


function __saac_add_eventlistener_paste(RPC)
{
	function pasteFocus(event)
	{
		var target = event.target;
		if(!target.classList.contains("saac_clipboard_insert_area"))
		{
			return;
		}
		
		var brd = target.whereParentWithBoundary(".saac_operation_insert_from_clipboard", ".saac_entry_point");//TODO saac_argument
		if(target === document.activeElement)
		{
			brd.classList.add("active");
			notify_user("info", "Beillesztés Ctrl+V megnyomására");
		}
		else
		{
			brd.classList.remove("active");
		}
	}
	
	document.body.addEventListener("focusin", pasteFocus);
	document.body.addEventListener("focusout", pasteFocus);
	
	document.body.addEventListener
	(
		"paste",
		function(event)
		{
			var target = event.target;
			//prevent handling unwanted events
			if(!target.classList.contains("saac_clipboard_insert_area"))
			{
				return;
			}
			event.preventDefault();
			
			//remove focus
			document.activeElement.blur();
			
			var data = event.clipboardData.getData('Text');
			
			var toCall = null;
			
			//the target is a textrate and the parent should be a "div"
			if(target.parentNode.classList.contains("saac_top_level_opearation"))
			{
				toCall = document.querySelector(".saac_function_container");
			}
			else
			{
				toCall = target
							.whereParentWithBoundary(".saac_argument", ".saac_entry_point")
							.querySelector(".saac_function_container");
			}
			
			__do_funtion_paste(toCall, data);
			
			return false;
		}
	);
}

function __saac_add_eventlistener_input_type(RPC)
{
	document.body.addEventListener
	(
		"click",
		function(event)
		{
			var target = event.target;
			//prevent handling unwanted events
			if(!target.classList.contains("saac_function_autocomplete"))
			{
				return;
			}
			
			if(event.ctrlKey)
			{
				event.preventDefault();
				if(target.nodeName == "INPUT")
				{
					target.alterTag("textarea");
				}
				else
				{
					target.classList.remove("saac_typeahead_wired");
					target.alterTag("input");
				}
				return false;
			}
		}
	);

}

/************************** init helper functions *****************************/

function __saac_init_show_menu(container, cls_selector, bool_show)
{
	if
	(
		is_valuable(container)
	&&
		is_valuable(cls_selector)
	&&
		is_valuable(bool_show)
	)
	{
		var tar = container.querySelector(cls_selector);
		if(is_valuable(tar))
		{
			if(bool_show)
			{
				tar.classList.remove("saac_no_show");
			}
			else
			{
				tar.classList.add("saac_no_show");
			}
		}
	}
}

function __saac_init_conf_get_root_type(cfg)
{
	if(is_valuable(cfg))
	{
		if(is_valuable(cfg.rootType))
		{
			var func = saac_get_function_by_id(cfg.rootType.func);
			if(!is_valuable(func))
			{
				func = saac_get_function_by_name(cfg.rootType.func);
			}
			
			if(!is_valuable(func))
			{
				throw "Function does'nt exists: "+cfg.rootType.func;
			}
			
			//checking argument boundary.
			var index = parseInt(cfg.rootType.argIndex);
			if(!is_valuable(index) && !isNaN(index))
			{
				throw "Invalid function argument index: "+cfg.rootType.argIndex;
			}
			
			if(index < 0 || index >= func.arguments.length)
			{
				throw "Function ('"+func.name+"') accepts only "+func.arguments.length+" arguments (length), "+index+" (index) given";
			}
			
			var ret = 
			{
				func: func.id+":"+index,
				type: func.arguments[index].type 
			};
			
			return ret;
		}
	}
	
	return null;
}

function __set_keys_value(obj, val, dot_dot_dot)
{
	var a = arguments;
	for(var i=2;i<arguments.length;++i)
	{
		obj[arguments[i]] = val;
	}
	
	return obj;
}

function saac_config_create_show_array(val)
{
	if(!is_valuable(val))
	{
		val = true;
	}
	
	return __set_keys_value
	(
		{},
		val,
		"clear",
		"cut",
		"insert",
		"copy",
		"save",
		"execute",
		"console",
		"browser",
		"editor"
	);
}

function __saac_init_options_visibility(root, cfg)
{
	console.log(["initvisible", cfg]);
	if(is_valuable(cfg) && is_valuable(cfg.show))
	{
		__saac_init_show_menu(root, ".saac_operation_remove", cfg.show.clear);
		__saac_init_show_menu(root, ".saac_operation_clipboard_cut", cfg.show.cut);
		__saac_init_show_menu(root, ".saac_operation_insert_from_clipboard", cfg.show.insert);
		__saac_init_show_menu(root, ".saac_operation_all_clipboard", cfg.show.copy);
		__saac_init_show_menu(root, ".saac_operation_save", cfg.show.save);
		__saac_init_show_menu(root, ".saac_operation_execute", cfg.show.execute);
		__saac_init_show_menu(root, ".saac_operation_show_console", cfg.show.console);
		__saac_init_show_menu(root, ".saac_project_file_container", cfg.show.browser);
		__saac_init_show_menu(root, ".saac_primary_container", cfg.show.editor);
		//__saac_init_show_menu(root, ".", cfg.show.);
	}	
	
	if(is_valuable(cfg) && is_valuable(cfg.embedConsole) && cfg.embedConsole)
	{
		console.log("EMBEDCONSOLE");
		__saac_init_show_menu(root, ".saac_hidden_console_content", cfg.embedConsole);
	}
}

/************************* Remote filesystem support **************************/

/*

FileEntry:
{
	type: ( regular, directory, special)
	size: (in bytes)
	name: (last part of file path)
	path: (full path including filename)
}

RemoteFileSystem:
{
	stat = function(path): FileEntry
	listFiles = function(path): FileEntry[]
	getFileContent = function(path): Blob
	setFileContent = function(path, Blob): boolean
}

*/


/******************** module/main contain initialization **********************/

function saac_get_console_root(/*Html.saac_entry_point*/ container)
{
	var tar = container.querySelector(".saac_console_root");
	
	//still embedded
	if(is_valuable(tar))
	{
		return tar;
	}
	else
	{
		return container.consoleDom;
	}
}

function saac_get_log_container(container)
{
	return saac_get_console_root(container).querySelector(".saac_log_container");
}

function saac_dispatch_server_event(container, packet)
{
	if(!container.classList.contains("saac_entry_point"))
	{
		container = saac_get_top_container(container);
	}
	
	var evt = JSON.parse(packet[0]);
	
	var _ = evt._;
	var func = evt.f;
	var args = evt.p;
	
	var listener = container.serverEvents;
	
	if(is_function(listener))
	{
		listener(func, args);
	}
	
	if("newLogLine" == func)
	{
		console.log("LOGGING LINE");
		var div = parseHtml("<div></div>");
		div.innerText = args[0];
		var cli = saac_get_log_container(container).appendChild(div);
	}
	//else
	{
		console.log(["server event:", container, _, func, args]);
	}
}

//initialize saac module 
function saac_init(RPC, after_init)
{
	RPC.hello();
	__saac_add_eventlistener_autocomplete(RPC);
	__saac_add_eventlistener_argument_sortable(RPC);
	__saac_add_eventlistener_clicks(RPC);
	__saac_add_eventlistener_paste(RPC);
	__saac_add_eventlistener_input_type(RPC);
	__saac_add_eventlistener_textarea(RPC);
	
	
	__saac_refresh_function_list
	(
		RPC,
		function()
		{
			if(is_function(after_init))
			{
				after_init();
			}
		}
	);
}

/**
 * 
 * config:
 * 	- show: [boolean]
 * 	{
 * 		clear, cut, insert, copy, execute, console, browser 
 * 	}
 * 	- embedConsole: if true, hidden console shown
 * 	- rootType: {func: "id_or_function_name", argIndex: "nth argument of function"}
 * 	- projectFileSupport: TODO
 * 	- loadFile: (path, used only if projectFileSupport provided)
 * */
function saac_initEntryPoint
(
	/*Html.saac_entry_point*/ root,
	cfg
)
{
	var root_type = __saac_init_conf_get_root_type(cfg);
	
	//setting the root type.
	if(null != root_type)
	{
		root.querySelector(".saac_root_accept_type").innerText = root_type.func;
		root.querySelector(".saac_root_type_name").innerText = saac_type_render(root_type.type);
	}
	
	///////////// Creating the main container ///////////// 
	root.querySelector(".saac_primary_container").appendChild(parseHtml(renderTemplate("saac/function_container")));
	
	__saac_init_options_visibility(root, cfg);
}


function saac_createEntryPoint(ROOT, cfg)
{
	ROOT.appendChild(parseHtml(renderTemplate("saac/main")));
	saac_initEntryPoint(ROOT, cfg);
}
