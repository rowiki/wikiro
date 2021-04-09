var program = require('commander');
const fs = require('fs');
const jsdom = require("jsdom");
const { JSDOM } = jsdom;

var allData = {};

program
  .version('1.0.0')
  .option('-u, --url [url]', 'URL to parse')
  .parse(process.argv)

async function processHouse(dom) {
  var data = {};
  const document = dom.window.document;
  console.log(dom.window.location.href);
  var h1 = document.querySelector("h1.entry-title");
  var title = h1.textContent;
  data["name"] = title;
  var textDiv = document.querySelector("div.entry-content");
  var text = textDiv.textContent;
  data["text"] = text;
  data["img"] = [];
  var imgDivs = document.querySelectorAll("div.su-custom-gallery-slide");
  for (let imgDiv of imgDivs) {
  	var imgUrl = imgDiv.querySelector("img").src;
	data["img"].push(imgUrl);
  }
  return data;
}

async function processSection(dom) {
  var data = {};
  const document = dom.window.document;
  console.log(dom.window.location.href);
  var ul = document.querySelector("ul.vm_elems");
  if (typeof ul === 'undefined' || ul === null) {
    //do we have a house at the top level? Process it
    var textDiv = document.querySelector("h1.entry-title");
    if (typeof textDiv !== 'undefined')
      var title = textDiv.textContent;
      data[title] = {
        "link": dom.window.location.href,
	"house": {}
      };
      data[title]["house"] = await processHouse(dom);
      return data;
  }
  var lis = ul.querySelectorAll("li");
  console.log(lis.length);
  
  for(let li of lis) {
    var n = li.querySelector("h3").textContent;
    console.log(n)
    var link = li.querySelector("a.seemore").href;
    data[n] = {
      "link": link,
      "house": {}
    };
    console.log(data[n])
    let dom = await JSDOM.fromURL(link);
    data[n]["house"] = await processHouse(dom);
    console.log(data);
  }
  return data;
}

async function processIndex(dom) {
  var data = {};
  const document = dom.window.document;
  console.log(dom.window.location.href);
  var ul = document.querySelector("ul.vm_elems");
  var lis = ul.querySelectorAll("li");
  console.log(lis);
  for(let li of lis) {
    n = li.querySelector("h3").textContent;
    link = li.querySelector("a.seemore").href;
    data[n] = {
      "link": link,
      "section": {}
    };
    let dom = await JSDOM.fromURL(link);
    data[n]["section"] = await processSection(dom);
    console.log(data);
  }
  return data;
}

console.log(program.url);
JSDOM.fromURL(program.url).then(dom => {
    processIndex(dom).then(data => {
      console.log(data);
      let datas = JSON.stringify(data);
      fs.writeFileSync('msat.json', datas);
    });
  });
