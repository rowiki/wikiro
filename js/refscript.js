var Manifest = '   RefScript version 42 (2015-12-10). This program is in the Public Domain. You can use it as you wish.   ';
var Tips = '  Pentru scurtarea scriptului, stergeţi între crestinortodox.ro şi youtube.com  -  sau folosiţi bookmarklet-ul din secţiunea care urmează după aceasta';
function toTitleCase(str) {
	var str = str.toLowerCase();
	return str.replace(/[^\s]+/g, function (word) {
		return word.replace(/^./, function (first) {
			return first.toUpperCase();
		});
	});
};
function f_process_Authors(P_Authors) {
	var P_Authors = P_Authors.replace(/\./g, '. ');
	var P_Authors = P_Authors.replace(/\n/g, '');
	var P_Authors = P_Authors.replace(/\t/g, '');
	var P_Authors = P_Authors.replace(/ +,/g, ',');
	var P_Authors = P_Authors.replace(/,/g, ', ');
	var P_Authors = P_Authors.replace(/  +/g, ' ');
	var P_Authors = P_Authors.replace(/^ +/, '');
	var P_Authors = P_Authors.replace(/ +$/, '');
	var P_Authors = P_Authors.replace(/^ +$/, '');
	var P_Authors = P_Authors.replace(/,*$/, '');
	var P_Authors = P_Authors.replace(/-/g, ' = ');
	if (AuthorNameTitleCase == 'yes')
		var P_Authors = toTitleCase(P_Authors);
	var P_Authors = P_Authors.replace(/ = /g, '-');
	var P_Authors = P_Authors.replace(/- /, '-');
	var P_Authors = P_Authors.replace(/Rl Online/, 'RL Online');
	if (P_Authors == 'Capital. Ro')
		var P_Authors = '';
	if (P_Authors == 'Jurnalul National')
		var P_Authors = '';
	if (P_Authors == 'Incomemagazine')
		var P_Authors = '';
	if (P_Authors == 'Adevarul')
		var P_Authors = '';
	if (P_Authors == 'Ziarul De Duminica')
		var P_Authors = '';
	if (P_Authors == 'Corespondenţi „adevărul”')
		var P_Authors = 'Corespondenţi „Adevărul”';
	var P_Authors = P_Authors.replace(/Corespondenţi „adevărul”/, 'Corespondenţi „Adevărul”');
	if (P_Authors == 'Agrointel. Ro')
		var P_Authors = '';
	if (P_Authors == 'Redactorii Bm')
		var P_Authors = '';
	if (P_Authors == 'Admin')
		var P_Authors = '';
	if (P_Authors == 'Redactia')
		var P_Authors = '';
	if (P_Authors == 'Razvan Pascu')
		var P_Authors = 'Răzvan Pascu';
	if (P_Authors == 'Răzvan Pascu')
		if (W_Newspaper == 'RazvanPascu.ro')
			var P_Authors = '';
	if (P_Authors == 'Loredana_user')
		var P_Authors = '';
	if (P_Authors.match(/@/))
		var P_Authors = '';
	if (P_Authors == '–')
		var P_Authors = '';
	if (P_Authors == '---')
		var P_Authors = '';
	if (P_Authors == 'Sursa: Crestinortodox. Ro')
		var P_Authors = '';
	if (P_Authors == 'Conf. Univ. Dr. Constantin Aslam')
		var P_Authors = 'conf. univ. dr. Constantin Aslam';
	if (P_Authors == 'Teodor Danalache')
		var P_Authors = 'Teodor Dănălache';
	if (P_Authors == 'Adrian Cocosila')
		var P_Authors = 'Adrian Cocoşilă';
	if (P_Authors == 'Stelian Gombos')
		var P_Authors = 'Stelian Gomboş';
	return P_Authors;
};
function f_process_Newspaper_Name() {
	var P_Newspaper = u.replace(/\.ro.*/, '.ro');
	var P_Newspaper = P_Newspaper.replace(/\.com.*/, '.com');
	var P_Newspaper = P_Newspaper.replace(/\.net.*/, '.net');
	var P_Newspaper = P_Newspaper.replace(/\.co.uk.*/, '.co.uk');
	var P_Newspaper = P_Newspaper.replace(/\.org.*/, '.org');
	var P_Newspaper = P_Newspaper.replace(/\.net.*/, '.net');
	var P_Newspaper = P_Newspaper.replace(/http:\/\/www./, '');
	var P_Newspaper = P_Newspaper.replace(/http:\/\//, '');
	var P_Newspaper = toTitleCase(P_Newspaper);
	if (P_Newspaper == 'Napocanews.ro')
		var P_Newspaper = 'Napoca News';
	if (P_Newspaper == 'Manastiriortodoxe.ro')
		var P_Newspaper = 'ManastiriOrtodoxe.ro';
	if (P_Newspaper == 'Bzi.ro')
		var P_Newspaper = 'Bună Ziua Iaşi';
	if (P_Newspaper == 'Stiintasitehnica.com')
		var xP_Newspaper = 'Știinţă şi Tehnică';
	if (P_Newspaper == 'Webcitation.org')
		var P_Newspaper = '';
	return P_Newspaper;
};
function f_process_Date(P_Date) {
	var P_Date = P_Date.toLowerCase();
	var P_Date = P_Date.replace(/\n/g, ' ');
	var P_Date = P_Date.replace(/\t/g, ' ');
	var P_Date = P_Date.replace(/^ +/, '');
	var P_Date = P_Date.replace(/ +$/, '');
	var P_Date = P_Date.replace(/^0/, '');
	var P_Date = P_Date.replace(/luni,? */, '');
	var P_Date = P_Date.replace(/marţi,? */, '');
	var P_Date = P_Date.replace(/miercuri,? */, '');
	var P_Date = P_Date.replace(/joi,? */, '');
	var P_Date = P_Date.replace(/vineri,? */, '');
	var P_Date = P_Date.replace(/sâmbătă,? */, '');
	var P_Date = P_Date.replace(/duminică,? */, '');
	var P_Date = P_Date.replace(/^ */, '');
	var P_Date = P_Date.replace(/\//g, '.');
	var P_Date = P_Date.replace(/-/g, '.');
	var P_Date = P_Date.replace(/\.01\./, ' ianuarie ');
	var P_Date = P_Date.replace(/\.02\./, ' februarie ');
	var P_Date = P_Date.replace(/\.03\./, ' martie ');
	var P_Date = P_Date.replace(/\.04\./, ' aprilie ');
	var P_Date = P_Date.replace(/\.05\./, ' mai ');
	var P_Date = P_Date.replace(/\.06\./, ' iunie ');
	var P_Date = P_Date.replace(/\.07\./, ' iulie ');
	var P_Date = P_Date.replace(/\.08\./, ' august ');
	var P_Date = P_Date.replace(/\.09\./, ' septembrie ');
	var P_Date = P_Date.replace(/\.10\./, ' octombrie ');
	var P_Date = P_Date.replace(/\.11\./, ' noiembrie ');
	var P_Date = P_Date.replace(/\.12\./, ' decembrie ');
	var P_Date = P_Date.replace(/ ian /, ' ianuarie ');
	var P_Date = P_Date.replace(/ feb /, ' februarie ');
	var P_Date = P_Date.replace(/ mar /, ' martie ');
	var P_Date = P_Date.replace(/ apr /, ' aprilie ');
	var P_Date = P_Date.replace(/ iun /, ' iunie ');
	var P_Date = P_Date.replace(/ iul /, ' iulie ');
	var P_Date = P_Date.replace(/ aug /, ' august ');
	var P_Date = P_Date.replace(/ sep /, ' septembrie ');
	var P_Date = P_Date.replace(/ oct /, ' octombrie ');
	var P_Date = P_Date.replace(/ nov /, ' noiembrie ');
	var P_Date = P_Date.replace(/ noi /, ' noiembrie ');
	var P_Date = P_Date.replace(/ dec /, ' decembrie ');
	var P_Date = P_Date.replace(/ january /i, ' ianuarie ');
	var P_Date = P_Date.replace(/ february /i, ' februarie ');
	var P_Date = P_Date.replace(/ march /i, ' martie ');
	var P_Date = P_Date.replace(/ april /i, ' aprilie ');
	var P_Date = P_Date.replace(/ may /i, ' mai ');
	var P_Date = P_Date.replace(/ june /i, ' iunie ');
	var P_Date = P_Date.replace(/ july /i, ' iulie ');
	var P_Date = P_Date.replace(/ august /i, ' august ');
	var P_Date = P_Date.replace(/ september /i, ' septembrie ');
	var P_Date = P_Date.replace(/ october /i, ' octombrie ');
	var P_Date = P_Date.replace(/ november /i, ' noiembrie ');
	var P_Date = P_Date.replace(/ december /i, ' decembrie ');
	var P_Date = P_Date.replace(/ jan /i, ' ianuarie ');
	var P_Date = P_Date.replace(/ jun /i, ' iunie ');
	var P_Date = P_Date.replace(/ jul /i, ' iulie ');
	var P_Date = P_Date.replace(/anul iv/i, 'anul IV');
	var P_Date = P_Date.replace(/anul iii/i, 'anul III');
	var P_Date = P_Date.replace(/anul ii/i, 'anul II');
	var P_Date = P_Date.replace(/anul i/i, 'anul I');
	return P_Date;
};
function f_process_EN_Date(P_Date) {
	var P_Date = P_Date.replace(/\n/g, ' ');
	var P_Date = P_Date.replace(/\t/g, ' ');
	var P_Date = P_Date.replace(/^ +/, '');
	var P_Date = P_Date.replace(/ +$/, '');
	var P_Date = P_Date.replace(/January 0/, 'January ');
	var P_Date = P_Date.replace(/February 0/, 'February ');
	var P_Date = P_Date.replace(/March 0/, 'March ');
	var P_Date = P_Date.replace(/April 0/, 'April ');
	var P_Date = P_Date.replace(/May 0/, 'May ');
	var P_Date = P_Date.replace(/June 0/, 'June ');
	var P_Date = P_Date.replace(/July 0/, 'July ');
	var P_Date = P_Date.replace(/August 0/, 'August ');
	var P_Date = P_Date.replace(/September 0/, 'September ');
	var P_Date = P_Date.replace(/October 0/, 'October ');
	var P_Date = P_Date.replace(/November 0/, 'November ');
	var P_Date = P_Date.replace(/December 0/, 'December ');
	var P_Date = P_Date.replace(/ Jan /, ' January ');
	var P_Date = P_Date.replace(/ Feb /, ' February ');
	var P_Date = P_Date.replace(/ Mar /, ' March ');
	var P_Date = P_Date.replace(/ Apr /, ' April ');
	var P_Date = P_Date.replace(/ Jun /, ' June ');
	var P_Date = P_Date.replace(/ Jul /, ' July ');
	var P_Date = P_Date.replace(/ Aug /, ' August ');
	var P_Date = P_Date.replace(/ Sep /, ' September ');
	var P_Date = P_Date.replace(/ Sept /, ' September ');
	var P_Date = P_Date.replace(/ Oct /, ' October ');
	var P_Date = P_Date.replace(/ Nov /, ' November ');
	var P_Date = P_Date.replace(/ Dec /, ' December ');
	return P_Date;
};
function f_process_Date_YMD(P_Date) {
	var P_Date = P_Date.replace(/ ianuarie /, '-01-');
	var P_Date = P_Date.replace(/ februarie /, '-02-');
	var P_Date = P_Date.replace(/ martie /, '-03-');
	var P_Date = P_Date.replace(/ aprilie /, '-04-');
	var P_Date = P_Date.replace(/ mai /, '-05-');
	var P_Date = P_Date.replace(/ iunie /, '-06-');
	var P_Date = P_Date.replace(/ iulie /, '-07-');
	var P_Date = P_Date.replace(/ august /, '-08-');
	var P_Date = P_Date.replace(/ septembrie /, '-09-');
	var P_Date = P_Date.replace(/ octombrie /, '-10-');
	var P_Date = P_Date.replace(/ noiembrie /, '-11-');
	var P_Date = P_Date.replace(/ decembrie /, '-12-');
	var P_Date = P_Date.replace(/(.*)-(.*)-(.*)/, '$3-$2-$1');
	var P_Date = P_Date.replace(/(.*)-(.)$/, '$1-0$2');
	var P_Date = P_Date.replace(/^ */, '');
	return P_Date;
};
function f_process_EN_Date_YMD(P_Date) {
	var P_Date = P_Date.replace(/January /, '01-');
	var P_Date = P_Date.replace(/February/, '02-');
	var P_Date = P_Date.replace(/March /, '03-');
	var P_Date = P_Date.replace(/April /, '04-');
	var P_Date = P_Date.replace(/May /, '05-');
	var P_Date = P_Date.replace(/June /, '06-');
	var P_Date = P_Date.replace(/July /, '07-');
	var P_Date = P_Date.replace(/August /, '08-');
	var P_Date = P_Date.replace(/September /, '09-');
	var P_Date = P_Date.replace(/October /, '10-');
	var P_Date = P_Date.replace(/November /, '11-');
	var P_Date = P_Date.replace(/December /, '12-');
	var P_Date = P_Date.replace(/st/, '');
	var P_Date = P_Date.replace(/nd/, '');
	var P_Date = P_Date.replace(/rd/, '');
	var P_Date = P_Date.replace(/th/, '');
	var P_Date = P_Date.replace(/(.*)-(.*), (.*)/, '$3-$1-$2');
	var P_Date = P_Date.replace(/(.*)-(.)$/, '$1-0$2');
	return P_Date;
};
function f_Repara_Poceli(P_Title) {
	var P_Title = P_Title.replace(/Ã¢â‚¬Å¾/g, '“');
	var P_Title = P_Title.replace(/â€”/g, '–');
	var P_Title = P_Title.replace(/â€™/g, '’');
	var P_Title = P_Title.replace(/â€˜/g, '‘');
	var P_Title = P_Title.replace(/â€/g, '”');
	var P_Title = P_Title.replace(/”ž/g, '„');
	var P_Title = P_Title.replace(/â€œ/g, '“');
	var P_Title = P_Title.replace(/â€ž/g, '„');
	var P_Title = P_Title.replace(/â€¦/g, '…');
	var P_Title = P_Title.replace(/Ã¶/g, 'ö');
	var P_Title = P_Title.replace(/Ã¼/g, 'ü');
	var P_Title = P_Title.replace(/Ãœ/g, 'Ü');
	var P_Title = P_Title.replace(/Ã©/g, 'é');
	var P_Title = P_Title.replace(/Ã®/g, 'î');
	var P_Title = P_Title.replace(/Ã¥/g, 'â');
	var P_Title = P_Title.replace(/ÃŽ/g, 'Î');
	var P_Title = P_Title.replace(/Å¢/g, 'Ț');
	var P_Title = P_Title.replace(/Èš/g, 'Ț');
	var P_Title = P_Title.replace(/Åž/g, 'Ș');
	var P_Title = P_Title.replace(/È˜/g, 'Ș');
	var P_Title = P_Title.replace(/Ä‚/g, 'Ă');
	var P_Title = P_Title.replace(/ï¿½/g, 'î');
	var P_Title = P_Title.replace(/Äƒ/g, 'ă');
	var P_Title = P_Title.replace(/Ã¢/g, 'â');
	var P_Title = P_Title.replace(/ÅŸ/g, 'ș');
	var P_Title = P_Title.replace(/È™/g, 'ș');
	var P_Title = P_Title.replace(/Å£/g, 'ț');
	var P_Title = P_Title.replace(/È›/g, 'ț');
	var P_Title = P_Title.replace(/ÇŽ/g, 'ă');
	var P_Title = P_Title.replace(/Ã.../g, 'Ă');
	var P_Title = P_Title.replace(/Â«/g, '«');
	var P_Title = P_Title.replace(/Â»/g, '»');
	var P_Title = P_Title.replace(/\uFFFD/g, 'î');
	var P_Title = P_Title.replace(/â€¢/g, '•');
	if (P_Title == '')
		var P_Title = WW_Title;
	var P_Title = P_Title.replace(/▶ /, '');
	return P_Title;
};
function f_process_URL(P_URL) {
	var P_URL = P_URL.replace(/https/, 'http');
	var P_URL = P_URL.replace(/\?utm.*/, '');
	var comment = '(Gândul)';
	var comment = '(EVZ)';
	var P_URL = P_URL.replace(/\?no_redirect=.*/, '');
	var P_URL = P_URL.replace(/\?iframe=.*/, '');
	var P_URL = P_URL.replace(/\?keepThis=.*/, '');
	return P_URL;
};
function f_process_REF_Name() {
	var P_Ref = u.replace(/\.ro.*/, '.ro');
	var P_Ref = P_Ref.replace(/\.com.*/, '.com');
	var P_Ref = P_Ref.replace(/\.net.*/, '.net');
	var P_Ref = P_Ref.replace(/\.org.*/, '.org');
	var P_Ref = P_Ref.replace(/\.co.uk.*/, '.co.uk');
	var P_Ref = P_Ref.replace(/http:\/\/www./, '');
	var P_Ref = P_Ref.replace(/http:\/\//, '');
	var P_Ref = P_Ref + '_';
	if (u.match(/evz.ro/))
		var P_Ref = 'evz';
	if (u.match(/evenimentulzilei.ro/))
		var P_Ref = 'evz';
	if (u.match(/adevarul.ro/))
		var P_Ref = 'adev';
	if (u.match(/romanialibera.ro/))
		var P_Ref = 'romlib';
	if (u.match(/jurnalul.ro/))
		var P_Ref = 'jurnalul';
	if (u.match(/gandul.info/))
		var P_Ref = 'gandul';
	if (u.match(/libertatea.ro/))
		var P_Ref = 'libertatea';
	return P_Ref;
};
function f_Array_to_String(myArray) {
	var myStr = '';
	for (i = 0; i < myArray.length; i++)
		var myStr = myStr + myArray[i] + '\n';
	return myStr;
};
var u = document.URL;
var d = document.body.innerHTML;
var comment = 'Flag variables here';
var SiteLN = 'ro';
var AuthorNameTitleCase = 'yes';
var comment = 'End Flag variables here';
var W_Authors = '';
var W_Date = '';
var W_Title = '';
var W_Newspaper = '';
var W_Source = '';
var sItalic = '\'\'';
var today = new Date();
var dd = today.getDate();
var mm = today.getMonth() + 1;
var yyyy = today.getFullYear();
var zdd = dd;
if (zdd < 10) {
	var zdd = '0' + zdd
};
if (mm < 10) {
	var mm = '0' + mm
};
var today = dd + '.' + mm + '.' + yyyy;
var yesterday = new Date(new Date().setDate(new Date().getDate() - 1));
var ydd = yesterday.getDate();
var ymm = yesterday.getMonth() + 1;
var yyyyy = yesterday.getFullYear();
if (ydd < 10) {
	var ydd = '0' + ydd
};
if (ymm < 10) {
	var ymm = '0' + ymm
};
var yesterday = ydd + '.' + ymm + '.' + yyyyy;
var today_EN = f_process_EN_Date(today);
var yesterday_EN = f_process_EN_Date(yesterday);
var today = f_process_Date(today);
var yesterday = f_process_Date(yesterday);
var WW_Title = document.title;
var WW_Title = WW_Title.replace(/ *\|.*/, '');
var WW_Newspaper = f_process_Newspaper_Name();
var W_URL = f_process_URL(u);
var dq = String.fromCharCode(34);
if (u.match(/webcitation.org/)) {
	var W_Title = 'arhiva WebCite';
	if (d.match(/An archive of this page should shortly be available at .*If the archiving/)) {
		var x = d.match(/An archive of this page should shortly be available at .*If the archiving/)[0];
		var x = x.replace(/<\/a>.*/, '');
		var W_URL = x.replace(/.*>/, '');
	};
} else {
	if (u.match(/coloramromania.evz.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Colorăm România/, '');
		var x = d.match(/li class=.data.*/)[0];
		var x = x.replace(/<\/li>.*/, '');
		var x = x.replace(/.*>/, '');
		var W_Date = x.replace(/.*,/, '');
		var x = d.match(/li class=.autor.*/)[0];
		var x = x.replace(/<\/li>.*/, '');
		var W_Authors = x.replace(/.*>/, '');
		var W_Newspaper = 'coloramromania.evz.ro';
	} else if (u.match(/evz.ro/)) {
		var x = d.match(/<h3.*>/)[0];
		var x = x.replace(/<h3>/, '');
		var x = x.replace(/\\/g, '');
		var W_Title = x.replace(/<\/h3>/, '');
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<\/div>/g, '<\/div>\n');
		var dd = dd.replace(/<div>/g, '\n<div>');
		if (dd.match(/<div class=.article-details./)) {
			var x = dd.match(/<div class=.article-details.*/)[0];
			var x = x.replace(/<\/a>/g, '<\/a>\n');
			var xa = x.match(/<a href=.*/g);
			for (i = 0; i < xa.length; i++) {
				var x = xa[i].replace(/\s*<\/a>.*/, '');
				if (i > 0)
					var W_Authors = W_Authors + ',';
				var W_Authors = W_Authors + x.replace(/.*>/, '');
			};
			var x = dd.match(/<div class=.article-details.*/)[0];
			var x = x.replace(/.*<\/a>\s*\| */, '');
			var x = x.replace(/\s*\|.*/, '');
			var W_Date = x.replace(/.*, */, '');
		};
		var W_Newspaper = 'Evenimentul zilei';
		var W_URL = W_URL.replace(/html#.*/, 'html');
	};
	if (u.match(/adevarul.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| adevarul.ro/, '');
		if (d.match(/time datetime=/)) {
			var x = d.replace(/\n/g, '');
			var x = x.replace(/\r/g, '');
			var x = x.replace(/<time/g, '\n<time');
			var x = x.replace(/<\/time>/g, '\n<\/time>');
			var x = x.match(/time datetime=.*/)[0];
			var x = x.replace(/,.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/<p>/, '');
		}
		if (d.match(/<a rel=.author./)) {
			var xa = d.match(/<a rel=.author.*/g);
			for (i = 0; i < xa.length; i++) {
				var x = xa[i].replace(/<.a><.span>.*/, '');
				if (i > 0)
					var W_Authors = W_Authors + ',';
				var W_Authors = W_Authors + x.replace(/.*>/, '');
			};
		};
		if (W_Authors == '') {
			if (W_Title.match(/\|/)) {
				var W_Authors = W_Title.replace(/.*\| */, '');
				var W_Title = W_Title.replace(/ *\|.*/, '');
			};
		};
		var W_Newspaper = 'Adevărul';
	};
	if (u.match(/adevarul.ro\/international\/foreign_policy/)) {
		var W_Newspaper = 'Adevărul - Foreign Policy';
	};
	if (u.match(/click.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Click/, '');
		if (d.match(/<span class=.meta_postdate.>/)) {
			var x = d.match(/<span class=.meta_postdate.>.*/)[0];
			var x = x.replace(/<\/span.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/ - .*/, '');
		}
		if (d.match(/<span class=.meta_author.>/)) {
			var x = d.match(/<span class=.meta_author.>.*/)[0];
			var x = x.replace(/<\/span.*/, '');
			var x = x.replace(/<\/b.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Click!';
	};
	if (u.match(/romanialibera.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/\s*\|\s*Romania Libera/, '');
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<\/div>/g, '<\/div>\n');
		var dd = dd.replace(/<div>/g, '\n<div>');
		if (dd.match(/<span class=.date.>/)) {
			var x = dd.match(/<span class=.date.*/)[0];
			var x = x.replace(/\s*<\/span>.*/, '');
			var W_Date = x.replace(/.*>\s*/, '');
		};
		var dd = dd.replace(/<a href>/g, '\n<a href>');
		var dd = dd.replace(/<\/span>/g, '<\/span>\n');
		if (d.match(/<span class=.author.>/)) {
			var x = dd.match(/<span class=.author.>.*/)[0];
			var x = x.replace(/\s*<\/span>/, '');
			var x = x.replace(/<\/a>/g, '<\/a>\n');
			var x = x.replace(/\s*<\/a>.*/g, '');
			var x = x.replace(/.*<a href=.*>/g, '');
			var W_Authors = x.replace(/\n/g, '');
		};
		var W_Newspaper = 'România liberă';
	};
	if (u.match(/jurnalul.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Jurnalul National/, '');
		if (d.match(/<h1>/)) {
			var x = d.match(/<h1>.*/)[0];
			var x = x.replace(/<h1>\s*/, '');
			var W_Title = x.replace(/\s*<.*/, '');
		};
		if (d.match(/class=.authors./)) {
			var x = d.match(/class=.authors.[^]*class=.clear./)[0];
			var x = x.match(/.*\d:/)[0];
			var x = x.replace(/ - .*/, '');
			var x = x.replace(/.\). onmouseout.*<\/a>/, '');
			var x = x.replace(/^\s*/, '');
			var x = x.replace(/ .*this,0,./, ' ');
			var W_Date = x;
			var x = d.match(/class=.authors.[^]*class=.clear./)[0];
			var x = x.replace(/class=.clear[^]*/, '');
			var x = x.replace(/<\/span>[^]*/, '');
			if (x.match(/href=.*/)) {
				var x = x.match(/href=.*/g);
				for (i = 0; i < x.length; i++) {
					var y = x[i].replace(/<.a>*/, '');
					var y = y.replace(/.*>/, '');
					if (i > 0)
						var W_Authors = W_Authors + ', ';
					var W_Authors = W_Authors + y;
				};
			};
		};
		if (W_Authors == '')
			if (d.match(/articolele autorului/)) {
				var x = d.match(/articolele autorului.*/)[0];
				var x = x.replace(/\/a>, <a/g, '/a>,\n <a');
				var x = x.match(/articolele autorului.*/g);
				for (i = 0; i < x.length; i++) {
					var y = x[i].replace(/articolele autorului /, '');
					var y = y.replace(/.>.*/, '');
					if (i > 0)
						var W_Authors = W_Authors + ', ';
					var W_Authors = W_Authors + y;
				};
			} else if (d.match(/id=.semnatura.*/)) {
				var x = d.match(/id=.semnatura.*/)[0];
				var x = x.replace(/.*semnatura.>/, '');
				var W_Authors = x.replace(/<.*/, '');
			} else {
				if (d.match(/smalltext/)) {
					var array_match = d.match(/smalltext.*/g);
					var x = array_match[0];
					if (array_match.length > 1)
						var x = array_match[1];
					var x = x.replace(/smalltext.>/, '');
					var W_Authors = x.replace(/<\/div.*/, '');
				};
				if (d.match(/li type=.square/)) {
					var x = d.match(/li type=.square.*/)[0];
					var x = x.replace(/.*>de/, '');
					var W_Authors = x.replace(/<.*/, '');
				};
			};
		var W_Newspaper = 'Jurnalul Naţional';
	};
	if (u.match(/dilemaveche.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Dilema Veche/, '');
		if (d.match(/<p>/)) {
			var x = d.match(/<p>.*/)[0];
			var x = x.replace(/\s*<\/p>.*/, '');
			var W_Date = x.replace(/.*>\s*/, '');
		};
		if (d.match(/href=..category.autor/)) {
			var x = d.match(/href=..category.autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		if (d.match(/href=.\/autor/)) {
			var x = d.match(/href=.\/autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Dilema Veche';
	};
	if (u.match(/cotidianul.ro/)) {
		var x = document.title;
		var x = x.replace(/ - Cotidianul/, '');
		var W_Title = x.replace(/ - Cotidianul/, '');
		var x = d.match(/.*class=.date[^]*<\/span>/)[0];
		var x = x.match(/.*<\/span>/)[0];
		var x = x.replace(/.*Publicat/, '');
		var x = x.replace(/.*,/, '');
		var W_Date = x.replace(/<.*/, '');
		if (d.match(/href=..autor/)) {
			var x = d.match(/href=..autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/.*>de/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		if (d.match(/<div class=.twelve author.>/)) {
			var xx = d.match(/<div class=.twelve author.>.*/)[0];
			var x = xx.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
			var x = xx.replace(/<\/div>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/.*, */, '');
		};
		if (W_Authors == 'Autori')
			var W_Authors = '';
		var W_Newspaper = 'Cotidianul';
	};
	if (u.match(/gandul.info/)) {
		var x = document.title;
		var x = x.replace(/ - Ieseanul/, '');
		var x = x.replace(/ - Banateanul/, '');
		var x = x.replace(/ - Clujeanul/, '');
		var x = x.replace(/ - Bihoreanul/, '');
		var W_Title = x.replace(/ - G(a|â)ndul/, '');
		var dateregex = /<div\s+class=.datetime.>\s*(.*?)\s*<\/div>/g;
		var datematches;
		var x = '';
		while (datematches = dateregex.exec(d)) {
			x = datematches[1];
			x = x.replace(/<p>\s*(P|p)ublicat(a|ă)?\s*<\/p>/g, '');
			x = x.replace(/<p\s*.*?>\s*\d{2}:\d{2}\s*<\/p>/g, '');
			x = x.replace(/<\/?p\s*(.*?)>/g, ' ');
			x = x.replace(/\s\s+/g, ' ');
		};
		var W_Date = x;
		var W_Authors = '';
		var authorregex = /<p\s+class=.name.\s*>(de)?\s*<a\s+href\s*=\s*(.*?)\s+title\s*=\s*.*?>(.*?)\s*<\/a>\s*<\/p>/g
		var authormatches;
		while (authormatches = authorregex.exec(d)) {
			x = authormatches[3];
			if (W_Authors != '') {
				W_Authors = W_Authors + ', '
			}
			W_Authors = W_Authors + x
		}
		var W_Newspaper = 'Gândul';
		if (u.match(/ieseanul.gandul.info/))
			var W_Newspaper = 'Ieşeanul';
		if (u.match(/banateanul.gandul.info/))
			var W_Newspaper = 'Bănăţeanul';
		if (u.match(/bihoreanul.gandul.info/))
			var W_Newspaper = 'Bihoreanul';
		if (u.match(/clujeanul.gandul.info/))
			var W_Newspaper = 'Clujeanul';
	};
	if (u.match(/zf.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Ziarul Financiar/, '');
		var authorElements = document.getElementsByClassName('articleMeta');
		var W_Authors, W_Date;
		if (authorElements != null && authorElements.length > 0) {
			var authorLis = authorElements[0].getElementsByTagName('li')
			if (authorLis != null && authorLis.length > 0) {
				for (var authorLiIdx = 0; authorLiIdx < authorLis.length; authorLiIdx++) {
					if (authorLis[authorLiIdx].class.indexOf('pull-right') >= 0) {
						continue;
					}
					if (authorLis[authorLiIdx].textContent().startsWith('Autor')) {
						var authorLiAs = authorLis[authorLiIdx].getElementsByTagName('a')
						if (authorLiAs != null && authorLiAs.length > 0) {
							W_Authors = authorLiAs[0].textContent();
						}
					} else {
						W_Date = authorLis[authorLiIdx].textContent();
					}
				}
			}
		}
		var W_Newspaper = 'Ziarul financiar';
	};
	if (u.match(/zf.ro\/ziarul-de-duminica/)) {
		var W_Newspaper = 'Ziarul de Duminică';
		var W_Title = W_Title.replace(/ *\/ de .*/, '');
	};
	if (u.match(/revista22.ro/)) {
		var W_Title = document.title;
		var titleregex = /<meta\s+property=.og:title.\s+content=.(.+?).\s+\/>/g;
		var metas = document.getElementsByTagName('meta');
		for (metasindex = 0; metasindex < metas.length; metasindex++) {
			if (metas[metasindex].getAttribute('property') == 'og:title') {
				W_Title = metas[metasindex].getAttribute('content');
			};
		};
		var dateregex = /<div\s+class=.date.>(<b>)?(\d{4}\-\d{2}\-\d{2})(<\/b>)?<\/div>/g;
		var datematches;
		var x = '';
		while (datematches = dateregex.exec(d)) {
			x = datematches[2];
		};
		var W_Date = x;
		var authorregexes = [];
		var authorregexesfunctionextractors = [];
		authorregexes.push(/<a href=.autor\.php\?s=.*?.> <span style=.color:#970606; font-family: 'Roboto', Arial, sans-serif; font-weight: 400; font-size:18px; letter-spacing: -1px;.> (.*?)<\/span><span style=.color:#970606; font-family: 'Roboto', Arial, sans-serif; font-weight: 900; font-size:18px; letter-spacing: -1px;.> (.*?) <\/span> <\/a>/g);
		authorregexesfunctionextractors.push(function(found_matches) { return found_matches[1] + ' ' + found_matches[2]});
		authorregexes.push(/<div class=.post_title.>(.*?)<\/div>\s*<div class=.date-category-comment. style=.padding:10px 0px;.>\s* <div class=.category.>(.*?)<b> <\/b><\/div>/g)
		authorregexesfunctionextractors.push(function(found_matches) { return found_matches[2]});
		var W_Authors = '';
		var authorregexindex = 0;
		while (W_Authors.length == 0 && authorregexindex < authorregexes.length) {
			var authormatches;
			console.log(d);
			while (authormatches = authorregexes[authorregexindex].exec(d)) {
				W_Authors = authorregexesfunctionextractors[authorregexindex](authormatches);
			};
			authorregexindex = authorregexindex + 1;
			console.log('authors=' + W_Authors)
		};
		var W_Newspaper = 'Revista 22';
	};
	if (u.match(/capital.ro/)) {
		var W_Title = document.title;
		if (d.match(/<h3>/)) {
			var x = d.match(/<h3>.*/)[0];
			var x = x.replace(/<\/h3>.*/, '');
			var W_Title = x.replace(/.*>/, '');
		};
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<\/div>/g, '<\/div>\n');
		var dd = dd.replace(/<div>/g, '\n<div>');
		if (dd.match(/<div class=.article-details./)) {
			var x = dd.match(/<div class=.article-details.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
			var x = dd.match(/<div class=.article-details.*/)[0];
			var x = x.replace(/.*<\/a>\s*\| */, '');
			var x = x.replace(/\s*\|.*/, '');
			var W_Date = x.replace(/.*, */, '');
		};
		var W_Newspaper = 'Capital';
	};
	if (u.match(/businessmagazin.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - BusinessMagazin/, '');
		var x = d.match(/<span>Postat la.*/)[0];
		var x = x.replace(/.*<strong>0?/, '');
		var W_Date = x.replace(/<.*/, '');
		if (d.match(/<a href=.\/autor/)) {
			var x = d.match(/<a href=.\/autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Business Magazin';
	};
	if (u.match(/financiarul.ro/)) {
		var x = document.title;
		var x = x.replace(/ \| FINANCIARUL - ultimele.*/, '');
		var W_Title = x.replace(/ \| Financiarul/, '');
		var x = d.match(/class=.fl date-calendar.*/)[0];
		var x = x.replace(/<\/p>/, '');
		var W_Date = x.replace(/.*>/, '');
		var W_Newspaper = 'financiarul.ro';
	};
	if (u.match(/wall-street.ro/)) {
		var W_Title = document.title;
		var x = d.match(/title=.Arhiva.*/)[0];
		var x = x.replace(/<\/a>/, '');
		var W_Date = x.replace(/.*>/g, '');
		var x = d.match(/<div class=.article-author-date.>[^]*<a href.*title=.Arhiva/)[0];
		var x = x.replace(/<a href.*title=.Arhiva.*/, '');
		var x = x.replace(/<\/a>.*/, '');
		var x = x.replace(/.*>/g, '');
		var x = x.replace(/<div class=.article-author-date.>/, '');
		var W_Authors = x.replace(/[ |\t|\n]*,[ |\t|\n]*$/, '');
		var W_Newspaper = 'wall-street.ro';
	};
	if (u.match(/[^s]fin.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \|.*/, '');
		var x = d.match(/class=.rightButton[^]*<a class=.plus/)[0];
		var x = x.replace(/class=.rightButton.*/, '');
		var W_Date = x.replace(/,[^]*/, '');
		if (d.match(/<a href=.\/autori/)) {
			var x = d.match(/<a href=.\/autori.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Financiarul';
	};
	if (u.match(/historia.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Historia/, '');
		var W_Date = '';
		if (d.match(/Autor:/)) {
			var x = d.match(/Autor:.*/)[0];
			var x = x.replace(/Autor: */, '');
			var W_Authors = x.replace(/ *\|/, '');
		};
		if (W_Authors.match(/Redactia/))
			var W_Authors = '';
		var W_Newspaper = 'Historia';
	};
	if (u.match(/ziare.com/)) {
		var d = d.replace(/<\/div>/g, '<\/div>\n');
		var d = d.replace(/<\/span>/g, '<\/span>\n');
		var x = document.title;
		var W_Title = x.replace(/ \| Ziare.com/, '');
		var x = d.match(/padding-top:8px;.*/)[0];
		var x = x.replace(/<.*/, '');
		var x = x.replace(/.*>/, '');
		var x = x.replace(/, ora .*/, '');
		var W_Date = x.replace(/.*,/, '');
		if (d.match(/Autor:/)) {
			var x = d.match(/Autor:.*/)[0];
			var x = x.replace(/.*<span>/, '');
			var W_Authors = x.replace(/<.*/, '');
		};
		if (d.match(/box_carusel_colaboratori/)) {
			var x = d.match(/box_carusel_colaboratori.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/<\/b>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Ziare.com';
	};
	if (u.match(/contributors.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Contributors/, '');
		var x = d.match(/class=.time.>.*/)[0];
		var x = x.replace(/<\/span>/, '');
		var W_Date = x.replace(/.*>/, '');
		if (d.match(/span class=.author authores/)) {
			var x = d.match(/span class=.author authores.*/)[0];
			var x = x.replace(/<\/a><\/span>/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Contributors.ro';
	};
	if (u.match(/hotnews.ro/)) {
		var x = document.title;
		var x = x.replace(/ - Arhiva.*/, '');
		var x = x.replace(/\(de.*/, '');
		var W_Title = x.replace(/ - HotNews.ro/, '');
		var x = d.match(/class=.data..*/)[0];
		var x = x.replace(/<\/span>.*/, '');
		var x = x.replace(/.*>/, '');
		var x = x.replace(/,/, ';');
		var x = x.replace(/,.*/, '');
		var W_Date = x.replace(/.*;/, '');
		var x = document.title;
		if (x.match(/\(de .*\)/)) {
			var x = x.match(/\(de.*\)/)[0];
			var x = x.replace(/\)/, '');
			var W_Authors = x.replace(/\(de /, '');
		} else {
			if (d.match(/class=.autor.>[^]*class=.categoria/)) {
				var x = d.match(/class=.autor.>[^]*class=.categoria/)[0];
				var x = x.replace(/<\/a>/, '\n');
				var x = x.match(/href=..*/)[0];
				var W_Authors = x.replace(/.*>/, '');
			} else {
				if (d.match(/<div class=.autor.> de <a rel=.nofollow. href=./)) {
					var x = d.match(/<div class=.autor.> de <a rel=.nofollow. href=..*<\/a>/)[0];
					var x = x.replace(/<\/a>/, '\n');
					var x = x.match(/href=..*/)[0];
					var W_Authors = x.replace(/.*>/, '');
				};
			};
		};
		var W_Newspaper = 'HotNews.ro';
	};
	if (u.match(/mediafax.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Mediafax.*/, '');
		var x = d.replace(/[\r\n]/g, '');
		var x = x.replace(/<\/dd>/g, '<\/dd>\n');
		var x = x.replace(/<dd/g, '\n<dd');
		var x = x.match(/<dd class=.date.*/)[0];
		var x = x.replace(/\s*<\/dd>/, '');
		var x = x.replace(/.*>\s*/, '');
		var xx = x;
		var x = x.replace(/.*,\s*/, '');
		var W_Date = x.replace(/[\(\)]/g, '');
		if (xx.match(/ieri/))
			var W_Date = 'ieri';
		if (xx.match(/ast.zi/))
			var W_Date = 'astăzi';
		if (d.match(/author_link/)) {
			var x = d.match(/author_link.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/<\/strong>.*/, '');
			var x = x.replace(/<\/strong>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Authors = x.replace(/ - Mediafax/, '');
		};
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<\/dd>/g, '<\/dd>\n');
		var dd = dd.replace(/<dd /g, '\n<dd ');
		if (dd.match(/<dd class=.last.*<strong>/)) {
			var x = dd.match(/<dd class=.last.*<strong.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/<\/strong>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Authors = x.replace(/ - Mediafax/, '');
		};
		var W_Newspaper = 'Mediafax';
	};
	if (u.match(/travel.descopera.ro/)) {
		var W_Title = document.title;
		var x = d.match(/<script src=.http:\/\/connect[^]*<\/small>\n<h1>/)[0];
		var x = x.match(/.*<\/small>/)[0];
		var x = x.replace(/<\/small>.*/, '');
		var W_Date = x.replace(/.*>/, '');
		if (d.match(/travel.descopera.ro\/autori/)) {
			var x = d.match(/travel.descopera.ro\/autori.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Descoperă - Travel';
	} else if (u.match(/descopera.ro/)) {
		var W_Title = document.title;
		var x = d.match(/class=.*date.*<.span>/)[0];
		var x = x.replace(/<\/span>.*/g, '');
		var W_Date = x.replace(/.*>/, '');
		if (d.match(/<span class=.author.><a href=/)) {
			var x = d.match(/<span class=.author.><a href=.*/)[0];
			var x = x.match(/href=.*<\/a>/)[0];
			var x = x.replace(/<\/a>/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		if (d.match(/Sursa:/)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/Sursa:/g, '\nSursa:');
			var x = x.replace(/<br/g, '\n<br');
			var x = x.replace(/<\/a>/g, '<\/a>\n');
			var x = x.match(/Sursa:.*/)[0];
			var x = x.replace(/.*http/, 'http');
			var W_Source = x.replace(new RegExp(dq + '.*'), '');
			if (W_Source.match(/mediafax.ro$/))
				var W_Source = 'chiull';
		};
		var W_Newspaper = 'Descoperă';
	};
	if (u.match(/www.revistamagazin.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/Revista Magazin - /, '');
		if (d.match(/<p align=.right.>/)) {
			var x = d.match(/<p align=.right.>.*>/)[0];
			var x = x.replace(/<\/p>/, '');
			var W_Authors = x.replace(/<p align=.right.>/, '');
		};
		var x = d.match(/class=.createdate.[^]*<\/td>/)[0];
		var x = x.match(/.*<\/td>/)[0];
		var x = x.replace(/<\/td>/, '');
		var W_Date = x.replace(/.*,/, '');
		var W_Newspaper = 'Revista Magazin';
	};
	if (u.match(/incomemagazine.ro/)) {
		var x = d.match(/<h1.*>/)[0];
		var x = x.replace(/.*<h1>/, '');
		var W_Title = x.replace(/<\/h1>.*/, '');
		var x = d.match(/Publicat de.*title=.*/)[0];
		var x = x.replace(/<\/p>.*/, '');
		var x = x.replace(/.*span> la */, '');
		var W_Date = x.replace(/,.*/, '');
		var x = d.match(/Publicat de.*title=.*/)[0];
		var x = x.replace(/.*title=./, '');
		var W_Authors = x.replace(new RegExp(dq + '.*'), '');
		var W_Newspaper = 'Income Magazine';
	};
	if (u.match(/9am.ro/)) {
		var W_Title = document.title;
		var x = d.match(/<div class=.article-date-author.>.*/)[0];
		var x = x.replace(/<\/div>/, '');
		var x = x.replace(/.*>/, '');
		var x = x.replace(/\ /g, ' ');
		var x = x.replace(/,.*/, '');
		var W_Date = x.replace(/.*\•/, '');
		var W_Date = x.replace(/.*•/, '');
		if (d.match(/\•/))
			var W_Authors = 'Aut' + x.replace(/\•.*/, ' ');
		if (d.match(/•/))
			var W_Authors = x.replace(/•.*/, ' ');
		var W_Newspaper = '9AM.ro';
	};
	if (u.match(/formula-as.ro/)) {
		var x = document.title;
		var x = x.replace(/ - Arhiva - Formula AS/, '');
		var x = x.replace(/ - Formula AS/, '');
		var x = x.replace(/ - Spiritualitate/, '');
		var x = x.replace(/ - Cultura/, '');
		var x = x.replace(/ - Acasa/, '');
		var x = x.replace(/ - Lumea romaneasca/, '');
		var x = x.replace(/ - Planete culturale/, '');
		var x = x.replace(/ - Asul de inima/, '');
		var x = x.replace(/ - Asii muzicii/, '');
		var x = x.replace(/ - Galeria vedetelor/, '');
		var x = x.replace(/ - Ce mai face .../, '');
		var x = x.replace(/ - Pop-Rock/, '');
		var x = x.replace(/ - Asii adolescentilor/, '');
		var x = x.replace(/ - Asii sportului/, '');
		var xx = x;
		var x = x.match(/Numarul.*/)[0];
		var x = x.replace(/Numarul/, 'Numărul');
		var xa = x.replace(/.* . /, '');
		var xn = x.replace(/ . .*/, '');
		var W_Date = xa + ', ' + xn;
		if (d.match(/class=.ico-author./)) {
			var x = d.match(/class=.ico-author..*/)[0];
			var x = x.replace(/.*title=./, '');
			var W_Authors = x.replace(new RegExp(dq + '.*'), '');
		};
		var W_Title = xx.replace(/ - Numarul.*/, '');
		var W_Newspaper = '\'\'Formula AS\'\'';
		var sItalic = '';
	};
	if (u.match(/money.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - portalul de business MONEY.ro/, '');
		var x = d.replace(/[\n\r]/g, '');
		if (x.match(/Autor:/)) {
			var x = x.match(/Autor:.*/)[0];
			var x = x.replace(/Autor:/, '');
			var W_Authors = x.replace(/<.*/, '');
		};
		var x = d.replace(/[\n\r]/g, '');
		if (x.match(/Publicat:/)) {
			var x = x.match(/Publicat:.*/)[0];
			var x = x.replace(/Publicat:/, '');
			var x = x.replace(/<.*/, '');
			var W_Date = x.replace(/,.*/, '');
		};
		var W_Newspaper = 'Money.ro';
	};
	if (u.match(/antena3.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Antena3/, '');
		var xx = d.replace(/[\r\n]/g, '@#@');
		var xx = xx.replace(/<a/, '\n<a');
		var xx = xx.replace(/<span/, '\n<span');
		var xx = xx.replace(/<\/span>/, '<\/span>\n');
		if (xx.match(/<a href=..autor/)) {
			var x = xx.match(/<a href=..autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
			var x = xx.match(/<a href=..autor.*/)[0];
			var x = x.replace(/.*<\/a>/, '');
			var x = x.replace(/.*@#@/, '');
			var W_Date = x.replace(/ *\ .*/, '');
		} else if (xx.match(/<span class=.fl.>/)) {
			var x = xx.match(/<span class=.fl.>.*/)[0];
			var x = x.replace(/.*<span class=.fl.>de/, '');
			var x = x.replace(/.*<span class=.fl.>/, '');
			var x = x.replace(/.*@#@/, '');
			var W_Date = x.replace(/ *\ .*/, '');
		};
		var W_Newspaper = 'Antena 3';
	};
	if (u.match(/cancan.ro/)) {
		var x = d.match(/<h1.*>/)[0];
		var x = x.replace(/.*<h1>/, '');
		var W_Title = x.replace(/<\/h1>.*/, '');
		if (d.match(/class=.article_date/)) {
			var x = d.match(/class=.article_date.*/)[0];
			var x = x.replace(/<\/div>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/,.*/, '');
		};
		if (d.match(/<a href=..autor/)) {
			var x = d.match(/<a href=..autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'CanCan';
	};
	if (u.match(/libertatea.ro/)) {
		var x = document.title;
		var x = x.replace(/ - Vedete de la noi.*/, '');
		var x = x.replace(/ - Ştiri interne.*/, '');
		var W_Title = x.replace(/ \| Libertatea.ro/, '');
		if (d.match(/<span>POSTAT:<.span>/)) {
			var x = d.match(/<span>POSTAT:<.span>.*/)[0];
			var x = x.replace(/<\/p>.*/, '');
			var x = x.replace(/.*> */, '');
			var W_Date = x.replace(/,.*/, '');
		};
		var W_Newspaper = 'Libertatea';
	};
	if (u.match(/realitatea.net/)) {
		var x = d.match(/<h1.*>/)[0];
		var x = x.replace(/.*<h1>/, '');
		var W_Title = x.replace(/<\/h1>.*/, '');
		if (d.match(/class=.header.>PUBLICAT/)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/<div/g, '\n<div');
			var x = x.match(/class=.header.>PUBLICAT.*/)[0];
			var x = x.replace(/.*<\/div>/, '');
			var x = x.replace(/ *\w*:.*$/, '');
			var W_Date = x.replace(/.*,/, '');
		};
		var W_Newspaper = 'Realitatea TV';
	};
	if (u.match(/rtv.net/)) {
		var x = d.match(/<h1.*>/)[0];
		var x = x.replace(/.*<h1>/, '');
		var W_Title = x.replace(/<\/h1>.*/, '');
		if (d.match(/class=.articleDate./)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/<div/g, '\n<div');
			var x = x.replace(/div>/g, 'div>\n');
			var x = x.match(/class=.articleDate..*/)[0];
			var x = x.replace(/<strong>.*/, '');
			var x = x.replace(/\|.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/,.*/, '');
		};
		if (d.match(/<strong.*rel=.author./)) {
			var x = d.match(/<strong.*rel=.author..*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'RTV';
	};
	if (u.match(/curierulnational.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Curierul National.*/, '');
		if (d.match(/span class=.left./)) {
			var x = d.match(/span class=.left..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/.*,/, '');
		};
		if (d.match(/citeste alte articole de acelasi autor/)) {
			var x = d.match(/.*citeste alte articole de acelasi autor/)[0];
			var x = x.replace(/.*<i>/, '');
			var x = x.replace(/<.*/, '');
			var x = x.replace(/[\(\)]/g, '');
			var W_Authors = x.replace(/ *\/.*/, '');
		};
		var W_Newspaper = 'Curierul Naţional';
	};
	if (u.match(/romlit.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Fundatia România Literara.*/, '');
		if (d.match(/.crumbTrail./)) {
			var x = d.match(/.crumbTrail..*/)[0];
			var xx = x.match(/www.romlit.ro\/arhiva_[12].*/)[0];
			var x = xx.replace(/_ro.>.*/, '');
			var xa = x.replace(/.*arhiva_/, '');
			var x = xx.replace(/.*.>Numarul /, '');
			var xn = x.replace(/<.*/, '');
			var W_Date = 'anul ' + xa + ', numărul ' + xn;
		};
		if (d.match(/div class=.wobjectRLArticle./)) {
			var x = d.match(/div class=.wobjectRLArticle..*/)[0];
			var x = x.match(/font color=.#8d8d8d..*/)[0];
			var x = x.replace(/<\/font><\/h1>.*/, '');
			var W_Authors = x.replace(/.*> *de */, '');
		};
		var W_Newspaper = '\'\'România literară\'\'';
		var sItalic = '';
	};
	if (u.match(/observatorcultural.ro/)) {
		var x = document.title;
		var x = x.replace(/ - Arhiva - Observatorcultural.ro.*/, '');
		var W_Title = x.replace(/ *- Observatorcultural.ro.*/, '');
		var x = W_Title.replace(/.* - Numarul/, 'Numărul');
		var x = x.replace(/ - /, ', ');
		var W_Date = x.replace(/ - /, ' ');
		var W_Title = W_Title.replace(/ - Numarul.*/, '');
		if (d.match(/<strong>Autor:/)) {
			var x = d.match(/<strong>Autor:.*/)[0];
			var x = x.replace(/ *<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = '\'\'Observator cultural\'\'';
		var sItalic = '';
	};
	if (u.match(/ziarullumina.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Ziarul Lumina/, '');
		var x = d.replace(/[\n\r]/g, '');
		if (x.match(/div class=.data-editie-inner./)) {
			var x = x.match(/div class=.data-editie-inner..*/)[0];
			var x = x.replace(/<\/div>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		if (d.match(/<a href=.\/autor/)) {
			var x = d.match(/<a href=.\/autor.*/)[0];
			var x = x.replace(/<\/div>.*/, '');
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Ziarul Lumina';
	};
	if (u.match(/ziaruldeiasi.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Ziarul Lumina/, '');
		if (d.match(/<div class=.article-date.>/)) {
			var x = d.match(/<div class=.article-date.>.*/)[0];
			var x = x.replace(/<\/div>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		if (d.match(/<div class=.article-author/)) {
			var x = d.match(/<div class=.article-author.*/)[0];
			var x = x.replace(/<\/div>.*/, '');
			var x = x.replace(/.*AUTOR: */, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Ziarul de Iaşi';
	};
	if (u.match(/cdep.ro/)) {
		if (d.match(/<td class=.headline./)) {
			var x = d.replace(/[\r\n]/g, '');
			var x = x.replace(/<\/td>/g, '\n<\/td>');
			var x = x.match(/<td class=.headline..*/)[0];
			var x = x.replace(/<br>/g, ' - ');
			var W_Title = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Camera Deputaţilor';
	};
	var DELETE1 = 'Here';
	if (u.match(/ziarulevenimentul.ro/)) {
		var x = document.title;
		var x = x.replace(/ \| Ziarul Evenimentul/, '');
		var W_Title = x.replace(/ \| Evenimentul/, '');
		if (d.match(/<div class=.data_publicare_stire.>/)) {
			var x = d.match(/<div class=.data_publicare_stire.>.*/)[0];
			var x = x.replace(/.*Data publicarii: */, '');
			var W_Date = x.replace(/ *<\/div>.*/, '');
		};
		var W_Newspaper = 'Evenimentul';
	};
	if (u.match(/amosnews.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Ziarul Lumina/, '');
		if (d.match(/<time property=.dc:issued./)) {
			var x = d.match(/<time property=.dc:issued..*/)[0];
			var x = x.replace(/<\/time>.*/, '');
			var x = x.replace(/\./g, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/ *-.*/, '');
		};
		var x = d.replace(/[\n\r]/g, '');
		if (x.match(/<span property=.dc:date dc:created./)) {
			var x = x.match(/<span property=.dc:date dc:created..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/ - .*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/.*, */, '');
			var x = x.replace(/\//g, '-');
			var W_Date = x.replace(/(..)-(..)-(..)/, '$2-$1-$3');
		};
		if (d.match(/>Rubrică realizată de /)) {
			var x = d.match(/>Rubrică realizată de .*/)[0];
			var x = x.replace(/–*<\/strong>.*/, '');
			var x = x.replace(/.*Rubrică realizată de /, '');
			var W_Authors = x.replace(/–*<.*/, '');
		};
		if (d.match(/>Comentariu realizat de /)) {
			var x = d.match(/>Comentariu realizat de .*/)[0];
			var x = x.replace(/–*<\/strong>.*/, '');
			var x = x.replace(/.*Comentariu realizat de /, '');
			var W_Authors = x.replace(/–*<.*/, '');
		};
		if (d.match(/<div class=.field-label.>Autor:/)) {
			var x = d.match(/<div class=.field-label.>Autor:.*/)[0];
			var x = x.replace(/–+/g, '–');
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Amos News';
	};
	if (u.match(/stirileprotv.ro/)) {
		var W_Title = document.title;
		if (d.match(/Data publicarii:/)) {
			var x = d.match(/Data publicarii:.*/)[0];
			var x = x.replace(/Data publicarii: */, '');
			var W_Date = x.replace(/ *<.*/, '');
		};
		var W_Newspaper = 'Pro TV';
	};
	if (u.match(/epochtimes-romania.com/)) {
		var W_Title = document.title;
		if (d.match(/<div id=.post_date.>/)) {
			var x = d.match(/<div id=.post_date.>.*/)[0];
			var x = x.replace(/.*<div id=.post_date.> */, '');
			var W_Date = x.replace(/ *<\/div>.*/, '');
		};
		if (d.match(/<div id=.author.>/)) {
			var x = d.match(/<div id=.author.>.*/)[0];
			var x = x.replace(/.*<div id=.author.> */, '');
			var W_Authors = x.replace(/ *<\/div>.*/, '');
		};
		var W_Newspaper = 'Epoch Times România';
	};
	if (u.match(/bbc.co.uk/)) {
		var x = document.title;
		var x = x.replace(/BBC NEWS \| Europe \| /, '');
		var x = x.replace(/.*\| */, '');
		var W_Title = x.replace(/BBC News - /, '');
		if (d.match(/<span class=.lu.>.*/)) {
			var x = d.match(/<span class=.lu.>.*/)[0];
			var x = x.replace(/<span class=.lu.>Last Updated: <\/span>/, '');
			var x = x.replace(/GMT.*/, '');
			var x = x.replace(/:.*/, '');
			var x = x.replace(/,/, '@');
			var x = x.replace(/.*@/, '');
			var W_Date = x.replace(/,.*/, '');
		} else if (d.match(/<span class=.date.>/)) {
			var x = d.match(/<span class=.date.>.*/)[0];
			var x = x.replace(/<span class=.date.>/, '');
			var W_Date = x.replace(/<\/span>/, '');
		};
		if (d.match(/<span class=.byl.>/)) {
			var x = d.match(/<span class=.byl.>[^]*<\/span>/)[0];
			var x = x.match(/By .*/)[0];
			var W_Authors = x.replace(/By /, '');
		} else if (d.match(/<span class=.byline-name.>/)) {
			var x = d.match(/<span class=.byline-name.>.*<\/span>/)[0];
			var x = x.replace(/<span class=.byline-name.>/, '');
			var W_Authors = x.replace(/<\/span>/, '');
		};
		var W_Newspaper = 'BBC';
	};
	if (u.match(/dailymail.co.uk/)) {
		if (d.match(/<h1>.*/)) {
			var x = d.match(/<h1>.*<\/h1>/)[0];
			var x = x.replace(/<.?h1>/g, '');
			var W_Title = x.replace(/<br>/g, '');
		};
		var x = d.replace(/[\n\r]/g, '');
		var x = x.replace(/<\/span>/g, '<\/span>\n');
		var x = x.replace(/<span>/g, '\n<span>');
		if (x.match(/<strong>PUBLISHED:/)) {
			var x = x.match(/<strong>PUBLISHED:.*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/.*,/, '');
		} else if (x.match(/<strong>UPDATED:/)) {
			var x = x.match(/<strong>UPDATED:.*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/.*,/, '');
		};
		if (d.match(/class=.author. rel=.nofollow./)) {
			var x = d.match(/class=.author. rel=.nofollow..*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Daily Mail';
		var SiteLN = 'en';
	};
	if (u.match(/arstechnica.com/)) {
		var x = document.title;
		var W_Title = x.replace(/ *\| *Ars Technica/, '');
		if (d.match(/ class=.date./)) {
			var x = d.match(/ class=.date..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/ *\w*:.*$/, '');
			var x = x.replace(/,/, '');
			var W_Date = x.replace(/(.*) (.*) (.*)/, '$2 $1 $3');
		};
		if (d.match(/class=.author-name./)) {
			var x = d.match(/class=.author-name.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		if (d.match(/class=.author./)) {
			var x = d.match(/class=.author..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Ars Technica';
		var SiteLN = 'en';
	};
	if (u.match(/tgdaily.com/)) {
		var x = document.title;
		var x = x.replace(/ \| TG Daily/, '');
		var W_Title = x.replace(/ – DIY Tech - TG Daily/, '');
		if (d.match(/<time datetime/)) {
			var x = d.match(/<time datetime.*/)[0];
			var x = x.replace(/<\/time>.*/, '');
			var x = x.replace(/.*>/, '');
			var W_Date = x.replace(/ - .*/, '');
			var x = d.match(/<time datetime.*/)[0];
			var x = x.replace(/.*meta-author.>/, '');
			var x = x.replace(/ *by */, '');
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/.*<a href=.*>/, '');
			var W_Authors = x.replace(/<.*/, '');
		};
		var W_Newspaper = 'TG Daily';
		var SiteLN = 'en';
	};
	if (u.match(/crestinortodox.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Ziarul Lumina/, '');
		if (d.match(/<div class=.clearfix mb5.>/)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/<\/p>/g, '<\/p>\n');
			var x = x.match(/<div class=.clearfix mb5.>.*/)[0];
			var x = x.replace(/<\/strong>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		if (d.match(/<p style=.text-align: justify; ?.><strong>/)) {
			var x = d.match(/<p style=.text-align: justify; ?.><strong>.*/g).length - 1;
			var x = d.match(/<p style=.text-align: justify; ?.><strong>.*/g)[x];
			var x = x.replace(/<\/strong>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		if (d.match(/.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.*<div class=.clear.>/)) {
			var x = d.match(/.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.nbsp;.*<div class=.clear.>/)[0];
			var x = x.replace(/.*\  */, '');
			var x = x.replace(/<.*/, '');
			if (W_Authors == '')
				var W_Authors = x;
			if (W_Authors.match(/Lumea credintei/))
				var W_Authors = '';
		};
		if (d.match(/<h2>Despre autor<.h2>/)) {
			var x = d.replace(/[\r\n]/g, '');
			var x = x.replace(/<h2>/g, '\n<h2>');
			var x = x.replace(/<\/strong>/g, '<\/strong>\n');
			var x = x.match(/<h2>Despre autor<.h2>.*/)[0];
			var x = x.replace(/<\/strong>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'CrestinOrtodox.ro';
		if (d.match(/prof\..*Stefan Grigorescu/i))
			var W_Authors = 'prof. Ștefan Grigorescu';
		if (d.match(/Dorina Zdroba/i))
			var W_Authors = 'Dorina Zdroba';
		if (d.match(/Gabriel Mateescu/i))
			var W_Authors = 'Gabriel Mateescu';
		if (d.match(/Florian Bichir/i))
			var W_Authors = 'Florian Bichir';
		if (d.match(/Razvan Bucuroiu/i))
			var W_Authors = 'Răzvan Bucuroiu';
		if (d.match(/Dumitru Manolache/i))
			var W_Authors = 'Dumitru Manolache';
		if (d.match(/Nicolae Balca/))
			var W_Authors = 'Nicolae Balca';
		if (d.match(/Teodor Danalache/i))
			var W_Authors = 'Teodor Dănălache';
		if (d.match(/Adrian Cocosila/i))
			var W_Authors = 'Adrian Cocoşilă';
		if (d.match(/<strong>N. Balca/))
			var W_Authors = 'N. Balca';
		if (d.match(/<strong>N. ?B./))
			var W_Authors = 'N. Balca';
		if (d.match(/Constantin Aslam/))
			var W_Authors = 'Conf. univ. dr. Constantin Aslam';
		if (d.match(/Vasile Musca/))
			var W_Authors = 'Vasile Musca';
		if (d.match(/C. Delvoye/))
			var W_Authors = 'Charles Delvoye';
		if (d.match(/Maria Chirculescu/))
			var W_Authors = 'Maria Chirculescu';
		if (d.match(/Baraganul Ortodox/))
			var W_Newspaper = 'Bărăganul Ortodox';
		if (d.match(/Lumea credintei, anul/)) {
			var x = d.match(/Lumea credintei, anul.*/)[0];
			var x = x.replace(/<\/p>.*/, '');
			var W_Newspaper = '\'\'Lumea credinţei\'\'';
			var x = x.replace(/Lumea credintei, */, '');
			var W_Date = x.replace(/<.*/, '');
			var sItalic = '';
		};
	};
	if (u.match(/razvanpascu.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Razvan Pascu/, '');
		var x = d.match(/<h2.*>/)[0];
		var x = x.replace(/<h2>/, '');
		var W_Title = x.replace(/<\/h2>.*/, '');
		if (d.match(/<p class=.date.>/)) {
			var x = d.match(/<p class=.date.>.*/)[0];
			var x = x.replace(/<\/p>.*/, '');
			var W_Date = x.replace(/.*<\/a> la /, '');
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'RazvanPascu.ro';
	};
	if (u.match(/sfin.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ *::.*/, '');
		var x = d.match(/class=.infoArticol[^]*ul class=.actiuniArticol/)[0];
		var x = x.replace(/[^]*<\/a>/, '');
		var x = x.replace(/<\/p>[^]*/, '');
		var x = x.replace(/\bpe /, '');
		var W_Date = x.replace(/,.*/, '');
		if (d.match(/www.sfin.ro\/autori/)) {
			var x = d.match(/www.sfin.ro\/autori.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Săptămâna Financiară';
	};
	if (u.match(/clicksanatate.ro/)) {
		var x = document.title;
		var x = x.replace(/ \| Click sanatate/, '');
		var W_Title = x.replace(/ \| Articole/, '');
		var x = d.match(/Autor: <span class=.comments..*>/)[0];
		var x = x.replace(/Autor: <span class=.comments.>/, '');
		var W_Authors = x.replace(/<\/span.*/, '');
		var x = d.match(/Autor: <span class=.comments.[^]* comentarii<\/span>/)[0];
		var x = x.replace(/Autor:.*/, '');
		var x = x.replace(/.*span.*/, '');
		var W_Date = x.replace(/,.*/, '');
		var W_Newspaper = 'Click! Sănătate';
	};
	if (u.match(/agrointel.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ * #agrointel.ro/, '');
		var x = d.match(/ rel=.author..*/)[0];
		var x = x.replace(/rel=.author.>/, '');
		var W_Authors = x.replace(/<.*/, '');
		var x = d.match(/title=.Articole din aceea.*/)[0];
		var x = x.replace(/<.*/, '');
		var W_Date = x.replace(/.*>/, '');
		var W_Newspaper = 'Agrointel.ro';
	};
	if (u.match(/doctorulzilei.ro/)) {
		var x = document.title;
		var x = x.replace(/ - Doctorul zilei.*/, '');
		var x = x.replace(/.*Feed Sanatate Evz, Ştiri - /, '');
		var W_Title = x.replace(/ — Doctorul zilei/, '');
		if (d.match(/<p class=.date./)) {
			var x = d.match(/<p class=.date..*/)[0];
			var x = x.replace(/<\/p>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/,/, '');
			var W_Date = x.replace(/ la .*/, '');
		};
		var bugs = 'can\'t get author name - example: http://www.doctorulzilei.ro/inainte-ca-inginerii-sa-ne-puna-la-dispozitie-aparatura-eram-epoca-bronzului-chirurgia-articulara/';
		var W_Newspaper = 'Doctorul zilei';
	};
	if (u.match(/animalzoo.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Animal Zoo/, '');
		if (d.match(/<p class=.date./)) {
			var x = d.match(/<p class=.date..*/)[0];
			var x = x.replace(/<\/p>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/,/, '');
			var W_Date = x.replace(/ la .*/, '');
		};
		var W_Newspaper = 'Animal Zoo';
	};
	if (u.match(/one.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Fundatia România Literara.*/, '');
		if (d.match(/<span><small>/)) {
			var x = d.match(/<span><small>.*/)[0];
			var x = x.replace(/<\/small>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		if (d.match(/class=.author./)) {
			var x = d.match(/class=.author..*/)[0];
			var x = x.replace(/<\/strong>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'ONE.ro';
	};
	if (u.match(/gq.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Fundatia România Literara.*/, '');
		if (d.match(/Articol de/)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/<\/ul>/g, '\n');
			var x = x.match(/Articol de *<strong>.*/)[0];
			var x = x.replace(/<\/li>/, '');
			var x = x.match(/<li>.*<.li>/)[0];
			var x = x.replace(/<li>/, '');
			var x = x.replace(/<\/li>/, '');
			var W_Date = x.replace(/.*- /, '');
		};
		if (d.match(/Articol de/)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/<\/a>/g, '\n');
			var x = x.match(/Articol de *<strong>.*/)[0];
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'GQ';
	};
	if (u.match(/b1.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ — Doctorul zilei/, '');
		var x = d.replace(/[\n\r]/g, '');
		var x = x.replace(/<span/g, '\n<span');
		var x = x.replace(/<\/span/g, '\n<\/span');
		var x = x.match(/Un articol de:.*/)[0];
		var x = x.replace(/.*<\/a> *\|*/, '');
		var x = x.replace(/<\/span>.*/, '');
		var W_Date = x.replace(/,.*/, '');
		var W_Newspaper = 'B1 TV';
	};
	if (u.match(/apropo.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ — Doctorul zilei/, '');
		if (d.match(/class=.NewsDate./)) {
			var x = d.match(/class=.NewsDate..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Apropo.ro';
	};
	if (u.match(/ziuaconstanta.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Fundatia România Literara.*/, '');
		if (d.match(/itemprop=.dateCreated./)) {
			var x = d.match(/itemprop=.dateCreated..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/ *\w*:.*$/, '');
			var W_Date = x.replace(/,/, '');
		};
		if (d.match(/itemprop=.author./)) {
			var x = d.match(/itemprop=.author..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Ziua de Constanţa';
	};
	if (u.match(/clujeanul.ro/)) {
		var x = d.replace(/[\n\r]/g, '');
		var x = x.replace(/<td/g, '\n<td');
		var x = x.replace(/<\/td>/g, '<\/td>\n');
		var x = x.match(/<td class=.titlu..*/)[0];
		var x = x.replace(/[ \t]*<\/td>/, '');
		var W_Title = x.replace(/.*>[ \t]*/, '');
		if (d.match(/td class=.textMic/)) {
			var x = d.match(/td class=.textMic.*/)[0];
			var x = x.replace(/.*<br>/, '');
			var W_Date = x.replace(/<.*/, '');
		};
		if (d.match(/javascript.deschideAutor/)) {
			var x = d.match(/javascript.deschideAutor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Clujeanul';
	};
	if (u.match(/ziuadecj.realitatea.net/)) {
		var W_Authors = '';
		var x = d.match(/<h1.*>/)[0];
		var x = x.replace(/.*<h1>/, '');
		var W_Title = x.replace(/<\/h1>.*/, '');
		if (d.match(/<div class=.author.>/)) {
			var x = d.replace(/[\n\r]/g, '');
			var x = x.replace(/<div/g, '\n<div');
			var x = x.match(/<div class=.author.>.*/)[0];
			var x = x.replace(/<\/div>.*/, '');
			var x = x.replace(/.*>,*/, '');
			var W_Date = x.replace(/,.*/, '');
		};
		var W_Newspaper = 'Ziua de Cluj';
	};
	if (u.match(/showbiz.ro/)) {
		var W_Title = document.title;
		if (d.match(/<span class=.date.>/)) {
			var x = d.match(/<span class=.date.>.*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		if (d.match(/articole-autor/)) {
			var x = d.match(/articole-autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'ShowBiz.ro';
	};
	if (u.match(/fanatik.ro/)) {
		var x = document.title;
		var x = x.replace(/ \| Sport si pariuri/, '');
		var W_Title = x.replace(/ - Fanatik/, '');
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<\/span>/g, '\n<\/span>');
		var dd = dd.replace(/<span>/g, '\n<span>');
		var x = dd.match(/<span class=.date-ico.>.*/)[0];
		var x = x.replace(/.*>/, '');
		var x = x.replace(/ *\w*:.*$/, '');
		var W_Date = x.replace(/,/, '');
		var x = dd.match(/<span class=.pencil.>.*/)[0];
		var W_Authors = x.replace(/.*>/, '');
		var W_Newspaper = 'Fanatik';
	};
	if (u.match(/revistatango.ro/)) {
		var W_Title = document.title;
		if (d.match(/<a href=.\/autor/)) {
			var x = d.match(/<a href=.\/autor.*/)[0];
			var x = x.replace(/.*<span>/, '');
			var W_Date = x.replace(/<.*/, '');
			var x = d.match(/<a href=.\/autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/Interviu de /, '');
			var x = x.replace(/ \/ Marea Dragoste/, '');
			var x = x.replace(/ • Fotografi[ei]:.*/, '');
			var x = x.replace(/; Foto:.*/, '');
			var x = x.replace(/ • Foto:.*/, '');
			var x = x.replace(/ ; Fotografie de.*/, '');
			var x = x.replace(/; *Fotografii: .*/, ''); ;
			Fotografii : var x = x.replace(/Text de /, '');
			var W_Authors = x.replace(/ - Fotografi[ie] de.*/, '');
		};
		if (d.match(/articole-autor/)) {
			var x = d.match(/articole-autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/Interviu de /, '');
			var W_Authors = x.replace(/ - Fotografii de.*/, '');
		};
		var W_Newspaper = 'Revista Tango';
	};
	if (u.match(/cariereonline.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Cariere/, '');
		if (d.match(/span class=.autor_articol./)) {
			var x = d.match(/span class=.autor_articol..*/)[0];
			var x = x.replace(/.*<span class=.font_12.>›*/, '');
			var x = x.replace(/<\/span>.*/, '');
			var x = x.replace(/ *\w*:.*$/, '');
			var x = x.replace(/.*, */, '');
			var W_Date = x.replace(/(....)-(..)-(..)/, '$3.$2.$1');
			var x = d.match(/span class=.autor_articol..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		if (d.match(/articole-autor/)) {
			var x = d.match(/articole-autor.*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'Cariere Online';
	};
	if (u.match(/qmagazine.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| - Q Magazine/, '');
		if (d.match(/<div id=.articlemeta./)) {
			var x = d.match(/<div id=.articlemeta..*/)[0];
			var x = x.replace(/ \| Niciun comentariu<\/div>.*/, '');
			var W_Date = x.replace(/.*\| */, '');
		};
		if (d.match(/<div id=.postauthor.>/)) {
			var x = d.match(/<div id=.postauthor.>.*/)[0];
			var x = x.replace(/<div id=.postauthor.>de */, '');
			var x = x.replace(/<div id=.postauthor.> */, '');
			var W_Authors = x.replace(/<.*/, '');
		};
		var W_Newspaper = 'Q Magazine';
	};
	if (u.match(/romaniatv.net/)) {
		var x = d.match(/<h1>.*/)[0];
		var x = x.replace(/<h1> */, '');
		var W_Title = x.replace(/<\/h1>.*/, '');
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<\/span>/g, '<\/span>\n');
		var dd = dd.replace(/<span/g, '\n<span');
		if (d.match(/Publicat: /)) {
			var x = d.match(/Publicat: .*/)[0];
			var x = x.replace(/.*Publicat: /, '');
			var x = x.replace(/ *\w*:.*$/, '');
			var x = x.replace(/.*, *0*/, '');
			var W_Date = x.replace(/.*, *0*/, '');
		};
		if (d.match(/. rel=.author./)) {
			var x = d.match(/. rel=.author..*/)[0];
			var x = x.replace(/<\/a>.*/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'RomaniaTV.net';
	};
	if (u.match(/viata-libera.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| - Q Magazine/, '');
		var dd = d.replace(/[\r\n]/g, '');
		var dd = dd.replace(/<span>/g, '\n<span>');
		var dd = dd.replace(/<\/span>/g, '<\/span>\n');
		if (dd.match(/<span class=.day.>/)) {
			var x = dd.match(/<span class=.day.>.*/)[0];
			var x = x.replace(/\s*<\/span>.*/, '');
			var x = x.replace(/<span class=.day.>/, '');
			var x = x.replace(/.*Publicat /, '');
			var x = x.replace(/.*, /, '');
			var x = x.replace(/ *\w*:.*$/, '');
			var W_Date = x;
		};
		if (d.match(/<h3>Articole/)) {
			var x = d.match(/<h3>Articole.*/)[0];
			var x = x.replace(/<h3>Articole scrise de /, '');
			var W_Authors = x.replace(/<.*/, '');
		};
		var W_Newspaper = 'Viaţa liberă Galaţi';
	};
	if (u.match(/telegrafonline.ro/)) {
		var added = ' 2014-03-19 ';
		var x = document.title;
		var W_Title = x.replace(/.*Telegraf Online Constanta - */, '');
		if (d.match(/<p class=.art_autor./)) {
			var x = d.match(/<p class=.art_autor..*/)[0];
			var x = x.replace(/ *<\/p>.*/, '');
			var x = x.replace(/.*>/, '');
			var x = x.replace(/.*, */, '');
			var W_Date = x;
		};
		if (d.match(/<a class=.art_autor./)) {
			var x = d.match(/.*<a class=.art_autor./)[0];
			var x = x.replace(/ *\(.*/, '');
			var W_Authors = x;
		};
		var W_Newspaper = 'Telegraf de Constanţa';
	};
	if (u.match(/www.ftr.ro/)) {
		var x = document.title;
		var W_Title = x.replace(/Foaia Transilvana - /, '');
		var x = d.match(/>Ultimele articole de .*>/)[0];
		var x = x.replace(/.*>Ultimele articole de/, '');
		var W_Authors = x.replace(/:<\/a>/, '');
		var x = d.match(/ Data: [^]* Ora: /)[0];
		var x = x.match(/.*<\/b>/)[0];
		var W_Date = x.replace(/<\/b>/, '');
		var W_Newspaper = 'Foaia Transilvană';
	};
	if (u.match(/stiintasitehnica.com/)) {
		var x = document.title;
		var W_Title = x.replace(/ \| Stiinta si Tehnica/, '');
		if (d.match(/<span class=.description-text/)) {
			var x = d.match(/<span class=.description-text.*/)[0];
			var x = x.replace(/\s*<\/span>.*/, '');
			var x = x.replace(/.*>\s*/, '');
			var W_Date = x.replace(/\s*\|.*/, '');
			var x = x.replace(/.*\|\s*/, '');
			var W_Authors = x.replace(/articol scris de\s*/, '');
		};
		var W_Newspaper = 'Știinţă şi Tehnică';
	};
	if (u.match(/businessweek.com/)) {
		var x = document.title;
		var W_Title = x.replace(/ - Businessweek/, '');
		if (d.match(/<div id=.publication_date.>/)) {
			var x = d.match(/<div id=.publication_date.>.*/)[0];
			var x = x.replace(/<div id=.publication_date.>/, '');
			var W_Date = x.replace(/<.*/, '');
		};
		if (d.match(/<div class=.author-name.>/)) {
			var x = d.match(/<div class=.author-name.>.*/)[1];
			var x = x.replace(/<div class=.author-name.>/, '');
			var x = x.replace(/By /, '');
			var x = x.replace(/and /, ',');
			var W_Authors = x.replace(/<.*/, '');
		};
		var W_Newspaper = 'BusinessWeek';
		var SiteLN = 'en';
	};
	if (u.match(/huffingtonpost.com/)) {
		var x = document.title;
		var W_Title = x;
		if (d.match(/itemprop=.datePublished./)) {
			var x = d.match(/itemprop=.datePublished..*/)[0];
			var x = x.replace(/itemprop=.datePublished.> */, '');
			var x = x.replace(/<.*/, '');
			var W_Date = x.replace(/ *\w*:.*$/, '');
		};
		if (d.match(/rel=.author.>/)) {
			var x = d.match(/rel=.author.>.*/)[0];
			var x = x.replace(/ *<\/a>/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'The Huffington Post';
		var SiteLN = 'en';
	};
	if (u.match(/huffingtonpost.ca/)) {
		var x = document.title;
		var W_Title = x;
		if (d.match(/itemprop=.datePublished./)) {
			var x = d.match(/itemprop=.datePublished..*/)[0];
			var x = x.replace(/itemprop=.datePublished.> */, '');
			var x = x.replace(/<.*/, '');
			var W_Date = x.replace(/ *\w*:.*$/, '');
		};
		if (d.match(/rel=.author.>/)) {
			var x = d.match(/rel=.author.>.*/)[0];
			var x = x.replace(/ *<\/a>/, '');
			var W_Authors = x.replace(/.*>/, '');
		};
		var W_Newspaper = 'The Huffington Post Canada';
		var SiteLN = 'en';
	};
	if (u.match(/youtube.com/)) {
		var x = document.title;
		var W_Title = x.replace(/ - YouTube/, '');
		if (d.match(/class=.watch-video-date./)) {
			var x = d.match(/class=.watch-video-date..*/)[0];
			var x = x.replace(/<\/span>.*/, '');
			var W_Date = x.replace(/.*>/, '');
		};
		if (d.match(/link itemprop=.url..*user\//)) {
			var x = d.match(/link itemprop=.url..*user\/.*/)[0];
			var x = x.replace(/'>.*/, '');
			var x = x.replace(/.*'/, '');
			var W_Authors = x.replace(/.*user\//, '');
			var AuthorNameTitleCase = 'no';
		};
		var W_Newspaper = 'YouTube';
		var SiteLN = 'en';
	};
	var DELETE2 = 'Here';
	if (u.match(/books.google/)) {
		var dd = d.replace(/[\r\n]/g, '');
		var x = dd.replace(/.*<td class=.metadata_label.>Title/, '');
		var x = x.replace(/<\/span>.*/, '');
		var W_Title = x.replace(/.*<span dir=.ltr.>/, '');
		var x = dd.replace(/.*class=.bookinfo_sectionwrap.><div>/, '');
		var x = x.replace(/<\/a><\/div><div><span dir=.ltr.>.*/, '');
		var x = x.replace(/<\/span><\/a><\/div><div>/, ', ');
		var x = x.replace(/ - <a.*/, ', ');
		var x = x.replace(/<\/span>/g, ', \n');
		var x = x.replace(/.*<span dir='ltr'>/g, '');
		var x = x.replace(/[\r\n]/g, '');
		var W_Authors = x.replace(/, $/, '');
		var W_Publisher = '';
		if (d.match(/<\/a><\/div><div><span dir=.ltr.>/)) {
			var x = dd.replace(/.*<\/a><\/div><div><span dir=.ltr.>/, '');
			var x = x.replace(/<span dir=.ltr.>.*/, '');
			var x = x.replace(/<a class.*/, '');
			var x = x.replace(/<\/div.*/, '');
			var x = x.replace(/ - $/, '');
			var x = x.replace(/<\/span>/, '');
			var x = x.replace(/^Ed\. /, 'Editura ');
			if (!(x.match(/^Editura /)))
				var x = 'Editura ' + x;
			var W_Publisher = x;
		};
		var s = '* \'\'' + W_Title + '\'\'';
		if (W_Authors != '')
			var s = s + ', ' + W_Authors;
		if (W_Publisher != '')
			var s = s + ', ' + W_Publisher;
		var s = s.replace(/, , /, ', ');
		var a = prompt('Google Books:', s);
		throw new Error();
	};
};
if (W_Title == '')
	var W_Title = WW_Title;
var W_Title = W_Title.replace(/\[/g, '(');
var W_Title = W_Title.replace(/\]/g, ')');
var W_Title = W_Title.replace(/^ +/, '');
var W_Title = W_Title.replace(/ +$/, '');
if (W_Date.match(/astăzi/i))
	var W_Date = today;
if (W_Date.match(/today/i))
	var W_Date = today_EN;
if (W_Date.match(/ieri/i))
	var W_Date = yesterday;
if (W_Date.match(/1 day ago/i))
	var W_Date = yesterday_EN;
if (SiteLN == 'ro') {
	var W_Date = f_process_Date(W_Date);
	var W_DateYMD = f_process_Date_YMD(W_Date);
}
if (SiteLN == 'en')
	var W_Date = f_process_EN_Date(W_Date);
var W_Authors = f_process_Authors(W_Authors);
var W_Title = f_Repara_Poceli(W_Title);
if (W_Newspaper == '')
	var W_Newspaper = WW_Newspaper;
if (sItalic == '') {
	var W_Newspaper = W_Newspaper + ' - ' + W_Date;
	var W_Date = '';
};
var s = '[' + W_URL + ' ' + W_Title + ']';
if (W_Date != '')
	var s = s + ', ' + W_Date;
if (W_Authors != '')
	var s = s + ', ' + W_Authors;
if (W_Newspaper != '')
	var s = s + ', ' + sItalic + W_Newspaper + sItalic;
var W_Ref_Name = f_process_REF_Name();
var ref1 = '<ref name=' + dq + W_Ref_Name + yyyy + '-' + mm + '-' + zdd + dq + '>';
var ref2 = ', accesat la ' + today + '</ref>';
var sr = ref1 + s + ref2;
var ref = '{{Citation | url=' + document.URL + '| title=' + W_Title + '| newspaper=' + W_Newspaper + '| date= ' + W_Date + '| author=' + W_Authors + '| accessdate=' + today + '}}';
var ref = ref1 + ref + '</ref>';
var sc = ref;
var comment = 'SD = Link and Date (in wiki format)';
var sd = '[' + W_URL + ' ' + sItalic + W_Newspaper + sItalic + ', ' + W_Date + ']';
var sd1 = W_Newspaper + ', ' + W_Date + W_Title + ' - ' + W_URL;
var sd1 = W_Date + ' - ' + W_Title + ' - ' + W_URL;
if (W_Title != 'arhiva WebCite')
	var s = '* ' + s;
else
	var s = '- ' + s;
if (W_Source.match(/http/))
	var s = s + ' - [' + W_Source + ' sursa]';
var comment = 'SLPD = Link, Publication, Date';
var slpd = sd;
var comment = 'SLAPD = Link, Author(s), Publication, Date';
var slapd = '[' + W_URL + ' ' + W_Authors + ', ' + sItalic + W_Newspaper + sItalic + ', ' + W_Date + ']';
var std = W_Title + ' - ' + W_Date;
var comment = 'STD = Title and Date';
var std = W_Title + ' - ' + W_Date;
var comment = 'STDYMD =  Title and Date (in YYYY-MM-DD format)';
var stdYMD = W_Title + ' - ' + W_DateYMD;
var comment = 'SDL = Date and Link (url)';
var sdl = W_Date + ' - ' + W_URL;
var comment = 'SDLYMD = Date (in YYYY-MM-DD format) and Link (url)';
var sdlymd = W_DateYMD + ' - ' + W_URL;
var s = prompt('Wiki-Reference', sc);
