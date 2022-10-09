// ==UserScript==
// @name         get_pdns
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  获取PDNS记录
// @author       liwu
// @match        http://10.95.54.85:30005/web/dinghai/clue_investigation
// @icon         data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==
// @grant        none
// ==/UserScript==
function get_pdns() {
    var all_data = [];
    var tbody = document.querySelectorAll('.ant-table-tbody')[1];
    var trs = tbody.children;

    for(var i = 0; i < trs.length; i++){
        var tr = trs[i];
        var tds = tr.children;
        var open_data = {};
        open_data.address = '';
        var flag = 0;
        for(var j = 0; j < tds.length; j++){
            // 记录ip
            if (j == 1) {
                // 已经记录一个 ip，就直接加上请求数量
                for(let k = 0; k < all_data.length; k++){
                    if(all_data[k].ip == tds[j].innerText) {
                        all_data[k].count = all_data[k].count + parseInt(tds[3].innerText);
                        flag = 1;
                        break;
                    }
                }
                if(flag == 1){
                    break;
                }
                // 没有就创建
                open_data.ip = tds[j].innerText;
            }
            // 记录请求数量
            else if (j == 3) {
                open_data.count = parseInt(tds[j].innerText);
            }
            // 记录地址
            else if ( j >= 5 && j <= 8) {
                open_data.address = open_data.address + tds[j].innerText;
            } else {
                continue
            }
        }
        if(flag == 0){
            all_data.push(open_data);
        }
    }

    // 降序排序
    for(let i = 0; i < all_data.length - 1; i++){
        for(let j = 0; j < all_data.length - i - 1; j++){
            if(all_data[j].count < all_data[j+1].count){
                var temp = all_data[j];
                all_data[j] = all_data[j+1];
                all_data[j+1] = temp;
            }
        }
    }

    // 输出前10个结果
    var s = '';
    for(let p = 0; p < 10;p++){
        s = s + all_data[p].ip + '->' + all_data[p].address + ':' + all_data[p].count + '\n';
    }
    console.log(s);
}


(function() {
    'use strict';
    document.onkeydown = function (event_e){
	if(window.event){
		event_e = window.event;
	}
	var int_keycode = event_e.charCode || event_e.keyCode;
    console.log(123);
	if(int_keycode == '13'){ //回车键：13
		get_pdns();//调用自己的函数
	}
}

})();