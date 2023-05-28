<script lang="ts">
    import { page} from "$app/stores";
    import {BASE_URL} from "$lib/kgdq";

    let organization = $page.url.searchParams.get("organization") ?? "";
    let gdqId = $page.url.searchParams.get("gdq_id") ?? "";
    let horaroId = $page.url.searchParams.get("horaro_id") ?? "";

    async function submit(e: Event) {
        e.preventDefault();

        let url = (document.getElementById("url") as HTMLInputElement).value;

        let res = await fetch(`${BASE_URL}/suggest/vod`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                url,
                organization,
                gdqId,
                horaroId
            })
        });

        if (res.ok) {
            alert("VOD submitted!");
        } else {
            alert("Something went wrong!");
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