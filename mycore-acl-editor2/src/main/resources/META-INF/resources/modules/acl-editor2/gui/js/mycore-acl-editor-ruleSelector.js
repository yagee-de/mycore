var RuleSelector = function(){
	var i18nKeys = [];
	var selector = $('<select/>',
		    {
		        size: 	'1',
		        class:	'input-xlarge access-rule'
		    });
	
	function formatSelect(item) {
		var span = $("<span></span>")
		span.text($(item.element).text());
		span.attr("title", $(item.element).attr("title"));
		return span;
	}
	
	return {
		init: 	function(rules, i18n){
			i18nKeys = i18n;
			var cla = this;
			$.each(rules, function(i, l) {
				cla.add(l.ruleID, l.desc, l.ruleSt)
			});
		},
		add: 	function(ruleID, ruleDesc, rule){
			selector.append("<option title='" + rule + "' value='" + ruleID + "'>" + ruleDesc + " (" + ruleID + ")</option>");
		},
		remove:	function(ruleID) {
			selector.find('option[value="' + ruleID + '"]').remove();
		},
		edit: function(ruleID, ruleDesc, ruleText) {
			selector.find('option[value="' + ruleID + '"]').attr("title", ruleText).html(ruleDesc + " (" + ruleID + ")");
		},
		update:	function() {
			var cla = this;
			$(".access-rule:not(.select2-container)").each(function() {
				var ruleID = $(this).select2("val") ;
				$(this).select2("destroy");
				cla.append(ruleID, $(this).parent());
				$(this).remove();
			});
		},
		append: function(ruleID, elem) {
			var newSelector = selector.clone();
			if (ruleID == "" || elem.hasClass("new-access-rule")){
				newSelector.prepend("<option value='' title='' selected>" + i18nKeys["ACLE.select.select"] + "</option>");
				newSelector.append("<option class='new-access-rule-option' value='new' title=''>" + i18nKeys["ACLE.select.newRule"] + "</option>");
			}
			newSelector.val(ruleID);
			newSelector.appendTo(elem);
			newSelector.select2({
				matcher: function(term, text, opt) {
						return text.toUpperCase().indexOf(term.toUpperCase())>=0
							|| opt.attr("title").toUpperCase().indexOf(term.toUpperCase())>=0;
					},
				formatResult: formatSelect
			});
		}
	};
}