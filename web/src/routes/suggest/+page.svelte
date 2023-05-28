<script lang="ts">
    import { page} from "$app/stores";
    import {BASE_URL} from "$lib/kgdq";

    let organization = $page.url.searchParams.get("organization") ?? "";
    let gdqId = $page.url.searchParams.get("gdq_id") ?? "";
    let horaroId = $page.url.searchParams.get("horaro_id") ?? "";

    async function submit(e: Event) {
        e.preventDefault();

        let url = (document.getElementById("url") as HTMLInputElement).value;

        let form_data = new FormData();
        form_data.append("url", url);
        form_data.append("organization", organization);
        form_data.append("gdqId", gdqId);
        form_data.append("horaroId", horaroId);

        let json_data = {
            url: url,
            organization: organization,
            gdqId: parseInt(gdqId),
            horaroId: horaroId
        };

        let res = await fetch(`${BASE_URL}/suggest/vod`, {
            credentials: "include",
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
                // "Content-Type": "multipart/form-data"
                // "Content-Type": "application/x-www-form-urlencoded"
            },
            body:
                JSON.stringify(json_data)
                //"gdq_id=4&horaro_id=3d5087ae07184e8c867d383fdafd7e9f&organization=hek&url=3"

        });

        console.log(res);

        console.log(await res.text())

        if (res.ok) {
            console.log("VOD submitted!");
        } else {
            console.log("Something went wrong!");
        }
    }

</script>

<h1>Suggest a VOD</h1>

<form>
    <label for="url">URL</label>
    <input type="text" id="url" name="url">

    <label for="organization">Organization</label>
    <input type="text" id="organization" name="organization" value="{ organization }" disabled>

    <label for="gdqId" >GDQ ID</label>
    <input type="text" id="gdqId" name="gdqId" value="{ gdqId }" disabled>

    <label for="horaroId" >Horaro ID</label>
    <input type="text" id="horaroId" name="horaroId" value="{ horaroId }" disabled>

    <button type="submit" on:click={submit}>Submit</button>
</form>