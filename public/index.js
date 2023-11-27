import {
    query_le,
    query_le_js,
    re_init_le_swipl,
    query_le_wasm,
    query_le_wasm_js,
    render_le_resp_with_guifier,
    clj_to_js,
    js_to_clj
  } from "./js/main.js";

const program = await fetch("program.le").then(x => x.text());
const data = await fetch("data.json").then(x => x.json());

// const query = "which person likes which hobby.";
// const query = "which person lives in which country.";
const query = "which person's current age is which number.";

const server_url = "https://le.dev.cclaw.legalese.com/";
const resp = await query_le_js(server_url, program, data, query);

// Query alternative wasm backend.
// await re_init_le_swipl("le.qlf");
// const resp = await query_le_wasm(program, data, query);

console.log("LE explanation tree: ", resp);

render_le_resp_with_guifier("guifier", resp);